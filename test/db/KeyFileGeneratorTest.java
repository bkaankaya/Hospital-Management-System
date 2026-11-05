package db;

import java.nio.file.*;
import java.util.Arrays;
import java.util.Base64;

/** Key-file uniqueness and length; no secret material appears in the output. */
public final class KeyFileGeneratorTest {
    private static void require(boolean b, String message) {
        if (!b) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("hospital-key-test-");
        Path one = dir.resolve("key-one.key");
        Path two = dir.resolve("key-two.key");
        try {
            KeyFileGenerator.create(one);
            KeyFileGenerator.create(two);
            byte[] a = Base64.getDecoder().decode(Files.readString(one));
            byte[] b = Base64.getDecoder().decode(Files.readString(two));
            require(a.length == 32 && b.length == 32 && !Arrays.equals(a, b),
                    "Key generator must produce distinct 256-bit random secrets");
            try {
                KeyFileGenerator.create(one);
                throw new AssertionError("Key generator overwrote an existing key");
            } catch (java.io.IOException expected) { }
        } finally {
            Files.deleteIfExists(one);
            Files.deleteIfExists(two);
            Files.deleteIfExists(dir);
        }
        System.out.println("Distinct key generation and overwrite refusal verified");
    }
}
