package db;

import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;

/** Invalid authentication tags must never publish the decrypted backup. */
public final class BackupVaultTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("hospital-backup-ci-");
        try {
            Path clear = dir.resolve("raw.sql");
            byte[] content = new byte[2048];
            new SecureRandom().nextBytes(content);
            Files.write(clear, content);
            Path archive = dir.resolve("backup.hbak");
            BackupVault.encrypt(clear, archive);
            require(Files.size(archive) > content.length,
                    "Backup was not packaged with its GCM nonce and tag");
            Path restored = dir.resolve("restored.sql");
            BackupVault.decrypt(archive, restored);
            require(Arrays.equals(content, Files.readAllBytes(restored)),
                    "Backup ciphertext did not recover identical bytes");

            Path corrupt = dir.resolve("corrupt.hbak");
            Files.copy(archive, corrupt);
            byte[] mutated = Files.readAllBytes(corrupt);
            mutated[mutated.length - 1] ^= 1;
            Files.write(corrupt, mutated);
            Path forbidden = dir.resolve("unverified.sql");
            try {
                BackupVault.decrypt(corrupt, forbidden);
                throw new AssertionError("Corrupt ciphertext accepted");
            } catch (javax.crypto.AEADBadTagException | java.io.IOException expected) {
                require(!Files.exists(forbidden), "Unauthenticated plaintext was published");
            }
            System.out.println("Encrypted backup recovery and tamper rejection passed");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.list(dir)) {
                for (Path p : (Iterable<Path>) paths::iterator) Files.deleteIfExists(p);
            }
            Files.deleteIfExists(dir);
        }
    }
}
