package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.ActivityLogEntry;
import tech.petclinix.logic.service.ActivityLogService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AdminActivityLogController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(AdminActivityLogController.class)
@Import(SecurityConfig.class)
class AdminActivityLogControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ActivityLogService activityLogService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a list of recent activity log entries for an authenticated admin. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getReturnsOkWithActivityLogList() throws Exception {
        //arrange
        var timestamp = LocalDateTime.of(2026, 8, 2, 10, 30);
        when(activityLogService.findRecent()).thenReturn(List.of(
                new ActivityLogEntry(1L, "alice", "PET_CREATED", timestamp),
                new ActivityLogEntry(2L, "bob", "USER_LOGIN", timestamp)
        ));

        //act + assert
        mockMvc.perform(get("/admin/activity-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].action").value("PET_CREATED"))
                .andExpect(jsonPath("$[1].username").value("bob"))
                .andExpect(jsonPath("$[1].action").value("USER_LOGIN"));
    }

    /** Returns 403 when the caller has the OWNER role instead of ADMIN. */
    @Test
    @WithMockUser(roles = "OWNER")
    void getReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/activity-logs"))
                .andExpect(status().isForbidden());
    }

    /** Returns 403 when the caller has the VET role instead of ADMIN. */
    @Test
    @WithMockUser(roles = "VET")
    void getReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/activity-logs"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void getReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/activity-logs"))
                .andExpect(status().isUnauthorized());
    }
}
