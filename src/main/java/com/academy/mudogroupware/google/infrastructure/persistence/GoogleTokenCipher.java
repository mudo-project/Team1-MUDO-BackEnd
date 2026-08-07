package com.academy.mudogroupware.google.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰을 DB에 평문으로 저장하지 않기 위한 AES-GCM 암복호화기.
 * 키는 JWT 서명 키와 분리된 전용 시크릿(GOOGLE_TOKEN_ENCRYPTION_KEY)을 SHA-256으로 해시해 32바이트로 만든다.
 */
@Component
public class GoogleTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleTokenCipher(@Value("${GOOGLE_TOKEN_ENCRYPTION_KEY}") String encryptionKey) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hashed, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화 키 초기화에 실패했습니다.", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰 암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined;
            try {
                combined = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("토큰 복호화에 실패했습니다.", e);
            }
            if (combined.length < IV_LENGTH_BYTES) {
                throw new IllegalStateException("토큰 복호화에 실패했습니다.");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰 복호화에 실패했습니다.", e);
        }
    }
}
