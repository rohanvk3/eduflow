package com.eduflow.backend.security;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTests {
    @Test
    void rejectsShortAndMalformedSecrets() {
        assertThatThrownBy(() -> new JwtService(Base64.getEncoder().encodeToString(new byte[31]), 3600))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 decoded bytes");
        assertThatThrownBy(() -> new JwtService("%%%", 3600))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService("", 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveExpiry() {
        String testKey = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> new JwtService(testKey, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("positive");
    }
}
