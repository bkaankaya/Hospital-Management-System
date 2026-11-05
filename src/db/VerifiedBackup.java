package db;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verify that a nonempty backup artifact exists and has the reviewed digest.
 * This is not itself proof of a successful restore; restore tests are separate.
 */
public final class VerifiedBackup {
    private VerifiedBackup() { }

    public static void verify(Path path, String sha256) throws Exception {
        if (path == null || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) < 64
                || sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("A nonempty regular backup and SHA-256 digest are required");
        }
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = input.read(chunk)) != -1) hash.update(chunk, 0, count);
        }
        if (!MessageDigest.isEqual(hash.digest(), HexFormat.of().parseHex(sha256))) {
            throw new IllegalArgumentException("Backup checksum verification failed");
        }
    }
}
