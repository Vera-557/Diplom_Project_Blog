package com.top511.vera.blog.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;


public final class PasswordUtil {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS       = 210_000;
    private static final int    SALT_BYTES       = 16;
    private static final int    KEY_LENGTH_BITS  = 256;
    private static final String FORMAT_PREFIX    = "pbkdf2";
    private static final String SEPARATOR        = "$";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}


    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Пароль не может быть null");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        return FORMAT_PREFIX + SEPARATOR
                + ITERATIONS + SEPARATOR
                + Base64.getEncoder().encodeToString(salt) + SEPARATOR
                + Base64.getEncoder().encodeToString(hash);
    }


    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (storedHash.startsWith(FORMAT_PREFIX + SEPARATOR)) {
            return matchesPbkdf2(rawPassword, storedHash);
        }
        return matchesLegacySha256(rawPassword, storedHash);
    }

    //  внутреннее

    private static boolean matchesPbkdf2(String rawPassword, String storedHash) {
        String[] parts = storedHash.split("\\" + SEPARATOR);
        if (parts.length != 4) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            // считаем, что пароль не подошёл
            return false;
        }
        if (iterations <= 0 || expected.length == 0) {
            return false;
        }
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static boolean matchesLegacySha256(String rawPassword, String storedHash) {
        String actual = legacySha256(rawPassword);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] pbkdf2(char[] password, byte[] salt,
                                 int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            try {
                return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                        .generateSecret(spec)
                        .getEncoded();
            } finally {
                // Не держим пароль в куче
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Не удалось вычислить PBKDF2-хеш", e);
        }
    }


    private static String legacySha256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}