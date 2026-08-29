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
import tech.petclinix.logic.domain.exception.InvalidAppointmentStatusException;
import tech.petclinix.logic.domain.AppointmentStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        var appt = new VetAppointment(1L, 20L, "Fluffy", "alice", LocalDateTime.of(2026, 5, 1, 9, 0), AppointmentStatus.BOOKED);
        when(vetAppointmentService.findAllByVet(new Username("drsmith")))
                .thenReturn(List.of(appt));

        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].petName").value("Fluffy"))
                .andExpect(jsonPath("$[0].ownerUsername").value("alice"))
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
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

    /** Returns 204 when the appointment is successfully confirmed by the vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void confirmReturnsNoContentOnSuccess() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/confirm"))
                .andExpect(status().isNoContent());
    }

    /** Returns 403 when an OWNER tries to confirm a vet appointment. */
    @Test
    @WithMockUser(roles = "OWNER")
    void confirmReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/confirm"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when confirming without authentication. */
    @Test
    void confirmReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/confirm"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 404 when confirming an appointment that does not belong to the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void confirmReturnsNotFoundWhenAppointmentBelongsToAnotherVet() throws Exception {
        //arrange
        doThrow(new NotFoundException("Appointment not found: 99"))
                .when(vetAppointmentService).confirmByVet(new Username("drsmith"), 99L);

        //act + assert
        mockMvc.perform(put("/vet/appointments/99/confirm"))
                .andExpect(status().isNotFound());
    }

    /** Returns 422 when confirming an appointment that is not currently BOOKED. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void confirmReturnsUnprocessableEntityWhenNotBooked() throws Exception {
        //arrange
        doThrow(new InvalidAppointmentStatusException(1L, AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED))
                .when(vetAppointmentService).confirmByVet(new Username("drsmith"), 1L);

        //act + assert
        mockMvc.perform(put("/vet/appointments/1/confirm"))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Returns 204 when the appointment is successfully marked no-show by the vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void markNoShowReturnsNoContentOnSuccess() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/no-show"))
                .andExpect(status().isNoContent());
    }

    /** Returns 403 when an OWNER tries to mark a vet appointment as no-show. */
    @Test
    @WithMockUser(roles = "OWNER")
    void markNoShowReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/no-show"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when marking no-show without authentication. */
    @Test
    void markNoShowReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/appointments/1/no-show"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 404 when marking no-show on an appointment that does not belong to the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void markNoShowReturnsNotFoundWhenAppointmentBelongsToAnotherVet() throws Exception {
        //arrange
        doThrow(new NotFoundException("Appointment not found: 99"))
                .when(vetAppointmentService).markNoShowByVet(new Username("drsmith"), 99L);

        //act + assert
        mockMvc.perform(put("/vet/appointments/99/no-show"))
                .andExpect(status().isNotFound());
    }

    /** Returns 422 when marking no-show on an appointment that is not currently CONFIRMED. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void markNoShowReturnsUnprocessableEntityWhenNotConfirmed() throws Exception {
        //arrange
        doThrow(new InvalidAppointmentStatusException(1L, AppointmentStatus.CONFIRMED, AppointmentStatus.BOOKED))
                .when(vetAppointmentService).markNoShowByVet(new Username("drsmith"), 1L);

        //act + assert
        mockMvc.perform(put("/vet/appointments/1/no-show"))
                .andExpect(status().isUnprocessableEntity());
    }
}
