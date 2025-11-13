package tech.petclinix.login.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@TestPropertySource(properties = {
        // Provide a strong base64 key that matches algorithm requirements (HS512).
        "jwt.secret=FaoYp2rCsi4aCR0qtSGnC2b76D5bvJtdJTJrpbrudXVUyQKbaCt2/OSDxvQb2S3JQf7gK0c6Zq2l1wS6fWwYxv6q8rJmH9n0P4tV8Y1bC3D4e5F6G7H8I9J0K==",
        "jwt.expirationMs=3600000"
})
public class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generateAndValidateToken_roundtrip() {
        //arrange
        String username = "test-user";

        //act
        String token = jwtUtil.generateToken(username);

        //assert
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(username);
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        //arrange+act
        boolean result = jwtUtil.validateToken("not-a-token");

        //assert
        assertThat(result).isFalse();
    }
}
