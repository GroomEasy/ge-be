package com.ceos.menual.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Zoom OAuth 토큰(access/refresh)을 DB에 저장할 때 암호화/복호화하기 위한 JPA AttributeConverter.
 *
 * <p>키는 코드에 하드코딩하지 않고, 런타임 환경변수/시크릿에서 주입해야 합니다.</p>
 *
 * <ul>
 *   <li>환경변수: {@code ZOOM_TOKEN_ENCRYPTION_KEY} (Base64 인코딩된 AES 키, 16/24/32 bytes)</li>
 * </ul>
 *
 * <p>저장 포맷: {@code v1:<base64url(iv)>:<base64url(ciphertextWithTag)>}</p>
 */
@Converter
public class ZoomTokenEncryptor implements AttributeConverter<String, String> {

    private static final String ENV_KEY = "ZOOM_TOKEN_ENCRYPTION_KEY";
    private static final String PREFIX = "v1:";
    private static final int IV_LEN_BYTES = 12; // 권장: 96-bit
    private static final int TAG_LEN_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Base64.Encoder B64URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (attribute.isBlank()) return attribute;
        if (attribute.startsWith(PREFIX)) return attribute; // 방어적(이중 암호화 방지)

        SecretKey key = loadKeyOrThrow();
        byte[] iv = new byte[IV_LEN_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
            cipher.updateAAD(PREFIX.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            return PREFIX + B64URL_ENC.encodeToString(iv) + ":" + B64URL_ENC.encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt Zoom token", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (dbData.isBlank()) return dbData;

        // 레거시(평문) 데이터는 그대로 반환 → 이후 저장 시 암호화로 자연스럽게 마이그레이션
        if (!dbData.startsWith(PREFIX)) return dbData;

        SecretKey key = loadKeyOrThrow();

        String payload = dbData.substring(PREFIX.length());
        int sep = payload.indexOf(':');
        if (sep <= 0 || sep == payload.length() - 1) {
            throw new IllegalStateException("Invalid encrypted token format");
        }

        byte[] iv = B64URL_DEC.decode(payload.substring(0, sep));
        byte[] ciphertext = B64URL_DEC.decode(payload.substring(sep + 1));

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
            cipher.updateAAD(PREFIX.getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt Zoom token", e);
        }
    }

    private static SecretKey loadKeyOrThrow() {
        // Spring 주입을 못 받으므로, (1) 환경변수 → (2) .env 파일 순서로 찾습니다.
        String b64 = System.getenv(ENV_KEY);
        if (b64 == null || b64.isBlank()) {
            b64 = readFromDotEnv(ENV_KEY);
        }
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("Missing encryption key: " + ENV_KEY + " (env var or .env)");
        }

        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(b64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Base64 in env var: " + ENV_KEY, e);
        }

        if (!(raw.length == 16 || raw.length == 24 || raw.length == 32)) {
            throw new IllegalStateException(
                "Invalid AES key length for " + ENV_KEY + ": " + raw.length + " bytes (expected 16/24/32)"
            );
        }

        return new SecretKeySpec(raw, "AES");
    }

    private static String readFromDotEnv(String key) {
        // 프로젝트 루트(working directory)의 ".env" 파일을 단순 key=value 형태로 읽습니다.
        // 주의: 키/값을 로그로 남기지 않습니다.
        Path path = Path.of(".env");
        if (!Files.exists(path)) return null;

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;

                String k = trimmed.substring(0, eq).trim();
                if (!key.equals(k)) continue;

                String v = trimmed.substring(eq + 1).trim();
                // 따옴표가 있으면 제거 (dotenv 호환)
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                return v;
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read .env for key: " + key, e);
        }
    }
}

