package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.InvalidCredentialsException;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AuthController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a token when credentials are valid. */
    @Test
    void loginReturnsOkWithTokenWhenCredentialsAreValid() throws Exception {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        when(userService.authenticate(new Username("alice"), "secret"))
                .thenReturn(domainUser);
        when(jwtUtil.generateToken(domainUser)).thenReturn("jwt-token-value");

        var requestBody = """
                {"username":"alice","password":"secret"}
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"));
    }

    /** Returns 401 when credentials are invalid. */
    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        //arrange
        when(userService.authenticate(any(), any())).thenThrow(new InvalidCredentialsException());

        var requestBody = """
                {"username":"alice","password":"wrong"}
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 400 when the request body is missing required fields. */
    @Test
    void loginReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
