package tech.petclinix.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.jwt.JwtUtil;
import tech.petclinix.web.controller.UsersController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the CORS configuration wired into {@link SecurityConfig}.
 *
 * Loads the real SecurityFilterChain (via @Import) around a mocked {@link UsersController}
 * to verify that cross-origin preflight requests are handled by the configured
 * CorsConfigurationSource — an allowed origin gets the expected Access-Control-* response
 * headers with no credentials allowed, and a disallowed origin is not echoed back.
 */
@WebMvcTest(UsersController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    JwtUtil jwtUtil;

    /** A preflight request from the allowed dev origin gets Access-Control-Allow-Origin/Methods but no Allow-Credentials header. */
    @Test
    void preflightFromAllowedOriginReturnsAllowedOriginAndMethodsWithoutCredentials() throws Exception {
        //act + assert
        mockMvc.perform(options("/users/aboutme")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    /** A preflight request from a disallowed origin does not get that origin echoed back in Access-Control-Allow-Origin. */
    @Test
    void preflightFromDisallowedOriginDoesNotEchoOriginHeader() throws Exception {
        //act + assert
        mockMvc.perform(options("/users/aboutme")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
