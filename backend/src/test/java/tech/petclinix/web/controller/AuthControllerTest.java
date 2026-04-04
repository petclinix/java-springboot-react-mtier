package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.InvalidCredentialsException;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.jwt.JwtUtil;
import tech.petclinix.web.dto.LoginRequest;
import tech.petclinix.web.dto.LoginResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthController}.
 *
 * Tests the controller in isolation. The HTTP contract is covered by {@link AuthControllerIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    UserService userService;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    AuthController authController;

    /** Returns 200 with a LoginResponse body when credentials are valid. */
    @Test
    void loginReturnsOkWithTokenWhenCredentialsAreValid() {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        var request = new LoginRequest("alice", "secret");
        when(userService.authenticate(new Username("alice"), "secret")).thenReturn(domainUser);
        when(jwtUtil.generateToken(domainUser)).thenReturn("generated-token");

        //act
        var response = authController.login(request);

        //assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        assertThat(response.getBody().token()).isEqualTo("generated-token");
    }

    /** Propagates InvalidCredentialsException when authentication fails. */
    @Test
    void loginPropagatesExceptionWhenCredentialsAreInvalid() {
        //arrange
        var request = new LoginRequest("alice", "wrong");
        when(userService.authenticate(new Username("alice"), "wrong"))
                .thenThrow(new InvalidCredentialsException());

        //act + assert
        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
