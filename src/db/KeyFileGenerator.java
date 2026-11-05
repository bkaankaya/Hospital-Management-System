package db;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;

/** Owner-only, create-once 256-bit key files for the trusted host. */
public final class KeyFileGenerator {
    private KeyFileGenerator() { }

    public static void create(Path destination) throws Exception {
        Path target = destination.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)
                || Files.isSymbolicLink(parent) || Files.exists(target)) {
            throw new java.io.IOException("Use a protected directory and a new file path");
        }
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        try {
            String material = Base64.getEncoder().encodeToString(key);
            if (Files.getFileStore(parent).supportsFileAttributeView("posix")) {
                // Create private from the first filesystem operation; do not
                // create a readable file and tighten permissions afterwards.
                java.nio.file.attribute.FileAttribute<java.util.Set<PosixFilePermission>> privateFile =
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                try (java.nio.channels.SeekableByteChannel channel = Files.newByteChannel(
                        target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                        privateFile)) {
                    channel.write(StandardCharsets.US_ASCII.encode(material));
                }
            } else {
                // On NTFS, administrators must preconfigure restrictive ACLs.
                // Fail closed instead of creating a broadly readable key.
                throw new java.io.IOException("Owner-only POSIX key creation unsupported; provision key via protected OS keystore");
            }
        } finally { java.util.Arrays.fill(key, (byte) 0); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: db.KeyFileGenerator /protected/directory/unique.key");
            System.exit(2);
            return;
        }
        create(Path.of(args[0]));
        System.out.println("Generated a new 256-bit key file (content not printed)");
    }
}
