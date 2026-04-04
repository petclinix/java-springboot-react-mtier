package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link UsersController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(UsersController.class)
@Import(SecurityConfig.class)
class UsersControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with user details when registration succeeds. */
    @Test
    void registerReturnsOkWithUserResponse() throws Exception {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        when(userService.register(new Username("alice"), "pass", UserType.OWNER))
                .thenReturn(domainUser);

        var body = """
                {"username":"alice","password":"pass","type":"OWNER"}
                """;

        //act + assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    /** Returns 409 when the username is already taken. */
    @Test
    void registerReturnsConflictWhenUsernameAlreadyTaken() throws Exception {
        //arrange
        when(userService.register(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("dup"));

        var body = """
                {"username":"alice","password":"pass","type":"OWNER"}
                """;

        //act + assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    /** Returns 400 when the request body is missing required fields. */
    @Test
    void registerReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        //act + assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 200 with current user details for an authenticated request. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void aboutmeReturnsOkWithUserDetails() throws Exception {
        //arrange
        var domainUser = new DomainUser(1L, "alice", UserType.OWNER, true);
        when(userService.findByUsername(new Username("alice")))
                .thenReturn(Optional.of(domainUser));

        //act + assert
        mockMvc.perform(get("/users/aboutme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    /** Returns 401 when no authentication is provided to the aboutme endpoint. */
    @Test
    void aboutmeReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/users/aboutme"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 404 when the authenticated user is not found in the database. */
    @Test
    @WithMockUser(username = "ghost", roles = "OWNER")
    void aboutmeReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        //arrange
        when(userService.findByUsername(new Username("ghost")))
                .thenReturn(Optional.empty());

        //act + assert
        mockMvc.perform(get("/users/aboutme"))
                .andExpect(status().isNotFound());
    }
}
