package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.StatsData;
import tech.petclinix.logic.service.StatsService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AdminStatsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(AdminStatsController.class)
@Import(SecurityConfig.class)
class AdminStatsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StatsService statsService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with system statistics for an authenticated admin. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getReturnsOkWithStatsForAdmin() throws Exception {
        //arrange
        var stats = new StatsData(5L, 3L, 10L, 20L,
                List.of(new StatsData.VetAppointmentCount("drsmith", 8L)));
        when(statsService.getStats()).thenReturn(stats);

        //act + assert
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwners").value(5))
                .andExpect(jsonPath("$.totalVets").value(3))
                .andExpect(jsonPath("$.totalPets").value(10))
                .andExpect(jsonPath("$.totalAppointments").value(20))
                .andExpect(jsonPath("$.appointmentsPerVet[0].vetUsername").value("drsmith"));
    }

    /** Returns 403 when the caller has the OWNER role instead of ADMIN. */
    @Test
    @WithMockUser(roles = "OWNER")
    void getReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    /** Returns 403 when the caller has the VET role instead of ADMIN. */
    @Test
    @WithMockUser(roles = "VET")
    void getReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void getReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
