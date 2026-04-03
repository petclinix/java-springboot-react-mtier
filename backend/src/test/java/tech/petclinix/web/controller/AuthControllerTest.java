package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.jwt.JwtUtil;
import tech.petclinix.web.dto.LoginRequest;
import tech.petclinix.web.dto.LoginResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthController} branching logic.
 *
 * Tests the {@code if (user.isPresent())} branch directly without loading
 * the web layer. The HTTP contract is covered by {@link AuthControllerIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    UserService userService;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    AuthController authController;

    /** Returns 200 with a LoginResponse body when the user is found and password matches. */
    @Test
    void loginReturnsOkWithTokenWhenUserPresent() {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        var request = new LoginRequest("alice", "secret");
        when(userService.authenticate(new Username("alice"), "secret"))
                .thenReturn(Optional.of(domainUser));
        when(jwtUtil.generateToken(domainUser)).thenReturn("generated-token");

        //act
        var response = authController.login(request);

        //assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        assertThat(((LoginResponse) response.getBody()).token()).isEqualTo("generated-token");
    }

    /** Returns 401 with an error message when authentication returns empty. */
    @Test
    void loginReturnsUnauthorizedWhenUserNotPresent() {
        //arrange
        var request = new LoginRequest("alice", "wrong");
        when(userService.authenticate(new Username("alice"), "wrong"))
                .thenReturn(Optional.empty());

        //act
        var response = authController.login(request);

        //assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(String.class);
    }
}
