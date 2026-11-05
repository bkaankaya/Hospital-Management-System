package db;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * Trusted-host-only, independently keyed AES-256-GCM backup archive.
 * Decryption stages output in an owner-only temporary file, and publishes it
 * only AFTER the authentication tag is verified. No third-party crypto library.
 */
public final class BackupVault {
    private static final byte[] MAGIC = "HOSPBAK2".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom RNG = new SecureRandom();
    private BackupVault() { }

    private static byte[] key() throws IOException {
        String name = System.getenv("HOSPITAL_BACKUP_KEY_FILE");
        if (name == null) throw new IOException("HOSPITAL_BACKUP_KEY_FILE is required");
        Path path = Path.of(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) throw new IOException("Backup key is not a protected file");
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            for (PosixFilePermission permission : Files.getPosixFilePermissions(path)) {
                if (permission.name().startsWith("OTHERS_")
                        || permission.name().startsWith("GROUP_")) {
                    throw new IOException("Backup key file must be owner-only");
                }
            }
        }
        byte[] secret;
        try { secret = Base64.getDecoder().decode(
                Files.readString(path, StandardCharsets.US_ASCII).trim()); }
        catch (IllegalArgumentException ex) { throw new IOException("Invalid backup key encoding"); }
        if (secret.length != 32) throw new IOException("Backup key must contain 32 random bytes");
        return secret;
    }

    private static Path temporary(Path destination) throws IOException {
        if (Files.exists(destination)) throw new IOException("Destination must not exist");
        Path absolute = destination.toAbsolutePath().normalize();
        Path directory = absolute.getParent();
        if (directory == null || !Files.isDirectory(directory))
            throw new IOException("Output directory missing");
        Path temp = Files.createTempFile(directory, ".hospital-vault-", ".tmp");
        if (Files.getFileStore(temp).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(temp,
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
        return temp;
    }

    private static void publish(Path temporary, Path destination) throws IOException {
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
    }

    public static void encrypt(Path input, Path destination) throws Exception {
        if (!Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(input) || Files.size(input) < 64)
            throw new IOException("Expected nonempty backup input");
        byte[] secret = key();
        Path temp = temporary(destination);
        boolean published = false;
        try {
            byte[] nonce = new byte[12];
            RNG.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"),
                    new GCMParameterSpec(128, nonce));
            try (InputStream source = Files.newInputStream(input);
                 OutputStream sink = Files.newOutputStream(temp)) {
                sink.write(MAGIC);
                sink.write(nonce);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    byte[] piece = cipher.update(buffer, 0, count);
                    if (piece != null) sink.write(piece);
                }
                sink.write(cipher.doFinal());
            }
            publish(temp, destination);
            published = true;
        } finally {
            Arrays.fill(secret, (byte) 0);
            if (!published) Files.deleteIfExists(temp);
        }
    }

    public static void decrypt(Path input, Path destination) throws Exception {
        if (!Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(input) || Files.size(input) < MAGIC.length + 12 + 16)
            throw new IOException("Expected authenticated backup archive");
        byte[] secret = key();
        Path temp = temporary(destination);
        boolean published = false;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            try (InputStream source = Files.newInputStream(input);
                 OutputStream sink = Files.newOutputStream(temp)) {
                byte[] magic = source.readNBytes(MAGIC.length);
                if (!Arrays.equals(magic, MAGIC)) throw new IOException("Unrecognized backup format");
                byte[] nonce = source.readNBytes(12);
                if (nonce.length != 12) throw new IOException("Invalid backup nonce");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, "AES"),
                        new GCMParameterSpec(128, nonce));
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    byte[] part = cipher.update(buffer, 0, count);
                    if (part != null) sink.write(part);
                }
                // An incorrect GCM authentication tag aborts before the output is published.
                sink.write(cipher.doFinal());
            }
            publish(temp, destination);
            published = true;
        } finally {
            Arrays.fill(secret, (byte) 0);
            if (!published) Files.deleteIfExists(temp);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: db.BackupVault encrypt|decrypt source destination");
            System.exit(2);
            return;
        }
        if ("encrypt".equals(args[0])) encrypt(Path.of(args[1]), Path.of(args[2]));
        else if ("decrypt".equals(args[0])) decrypt(Path.of(args[1]), Path.of(args[2]));
        else throw new IllegalArgumentException("Invalid backup operation");
        System.out.println("Authenticated backup operation completed");
    }
}
