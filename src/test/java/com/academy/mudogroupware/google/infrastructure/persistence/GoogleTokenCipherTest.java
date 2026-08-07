package com.academy.mudogroupware.google.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GoogleTokenCipherTest {

    private final GoogleTokenCipher cipher = new GoogleTokenCipher("test-encryption-key-value");

    @Test
    void decryptReturnsOriginalPlainText() {
        String encrypted = cipher.encrypt("1//refresh-token-value");

        assertThat(cipher.decrypt(encrypted)).isEqualTo("1//refresh-token-value");
    }

    @Test
    void encryptProducesDifferentCipherTextForSamePlainText() {
        String first = cipher.encrypt("same-token");
        String second = cipher.encrypt("same-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("same-token");
        assertThat(cipher.decrypt(second)).isEqualTo("same-token");
    }

    @Test
    void decryptThrowsIllegalStateExceptionWhenInputIsNotValidBase64() {
        assertThatThrownBy(() -> cipher.decrypt("not-valid-base64!!"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decryptThrowsIllegalStateExceptionWhenInputIsTooShortForIv() {
        assertThatThrownBy(() -> cipher.decrypt("YQ=="))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructorThrowsWhenEncryptionKeyIsBlank() {
        assertThatThrownBy(() -> new GoogleTokenCipher(" "))
                .isInstanceOf(IllegalStateException.class);
    }
}
