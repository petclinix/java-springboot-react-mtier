package tech.petclinix.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;

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
        DomainUser user =  new DomainUser(1l,"test-user", UserType.OWNER, true);

        //act
        String token = jwtUtil.generateToken(user);

        //assert
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(user.username());
    }

    @Test
    void getScopeFromToken_returnsCorrectScope() {
        //arrange
        DomainUser owner = new DomainUser(2l, "owner-user", UserType.OWNER, true);
        DomainUser vet   = new DomainUser(3l, "vet-user",   UserType.VET,   true);

        //act
        String ownerToken = jwtUtil.generateToken(owner);
        String vetToken   = jwtUtil.generateToken(vet);

        //assert
        assertThat(jwtUtil.getScopeFromToken(ownerToken)).isEqualTo("OWNER");
        assertThat(jwtUtil.getScopeFromToken(vetToken)).isEqualTo("VET");
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        //arrange+act
        boolean result = jwtUtil.validateToken("not-a-token");

        //assert
        assertThat(result).isFalse();
    }
}
