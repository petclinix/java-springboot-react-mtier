package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetAppointment;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.VetAppointmentService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link VetAppointmentsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(VetAppointmentsController.class)
@Import(SecurityConfig.class)
class VetAppointmentsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VetAppointmentService vetAppointmentService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a list of appointments for the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void listReturnsOkWithAppointmentList() throws Exception {
        //arrange
        var appt = new VetAppointment(1L, 20L, "Fluffy", "alice", LocalDateTime.of(2026, 5, 1, 9, 0));
        when(vetAppointmentService.findAllByVet(new Username("drsmith")))
                .thenReturn(List.of(appt));

        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].petName").value("Fluffy"))
                .andExpect(jsonPath("$[0].ownerUsername").value("alice"));
    }

    /** Returns 403 when the caller has the OWNER role instead of VET. */
    @Test
    @WithMockUser(roles = "OWNER")
    void listReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void listReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 204 when the appointment is successfully cancelled by the vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void cancelReturnsNoContentOnSuccess() throws Exception {
        //act + assert
        mockMvc.perform(delete("/vet/appointments/1"))
                .andExpect(status().isNoContent());
    }

    /** Returns 404 when the appointment does not belong to the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void cancelReturnsNotFoundWhenAppointmentBelongsToAnotherVet() throws Exception {
        //arrange
        doThrow(new NotFoundException("Appointment not found: 99"))
                .when(vetAppointmentService).cancelByVet(new Username("drsmith"), 99L);

        //act + assert
        mockMvc.perform(delete("/vet/appointments/99"))
                .andExpect(status().isNotFound());
    }

    /** Returns 403 when an OWNER tries to cancel a vet appointment. */
    @Test
    @WithMockUser(roles = "OWNER")
    void cancelReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(delete("/vet/appointments/1"))
                .andExpect(status().isForbidden());
    }
}
