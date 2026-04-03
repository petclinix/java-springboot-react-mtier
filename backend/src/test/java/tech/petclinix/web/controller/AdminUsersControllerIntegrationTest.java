package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AdminUsersController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(AdminUsersController.class)
@Import(SecurityConfig.class)
class AdminUsersControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a list of all users for an authenticated admin. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllReturnsOkWithUserList() throws Exception {
        //arrange
        when(userService.findAll()).thenReturn(List.of(
                new DomainUser(1L, "alice", UserType.OWNER, true),
                new DomainUser(2L, "drsmith", UserType.VET, true)
        ));

        //act + assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].username").value("drsmith"));
    }

    /** Returns 403 when the caller has the OWNER role instead of ADMIN. */
    @Test
    @WithMockUser(roles = "OWNER")
    void getAllReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void getAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with the deactivated user when deactivation succeeds. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateReturnsOkWithUpdatedUser() throws Exception {
        //arrange
        when(userService.deactivate(1L))
                .thenReturn(new DomainUser(1L, "alice", UserType.OWNER, false));

        //act + assert
        mockMvc.perform(put("/admin/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.active").value(false));
    }

    /** Returns 404 when the user to deactivate does not exist. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        //arrange
        when(userService.deactivate(99L))
                .thenThrow(new NotFoundException("User not found: 99"));

        //act + assert
        mockMvc.perform(put("/admin/users/99/deactivate"))
                .andExpect(status().isNotFound());
    }

    /** Returns 200 with the activated user when activation succeeds. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void activateReturnsOkWithUpdatedUser() throws Exception {
        //arrange
        when(userService.activate(1L))
                .thenReturn(new DomainUser(1L, "alice", UserType.OWNER, true));

        //act + assert
        mockMvc.perform(put("/admin/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.active").value(true));
    }

    /** Returns 404 when the user to activate does not exist. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void activateReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        //arrange
        when(userService.activate(99L))
                .thenThrow(new NotFoundException("User not found: 99"));

        //act + assert
        mockMvc.perform(put("/admin/users/99/activate"))
                .andExpect(status().isNotFound());
    }
}
