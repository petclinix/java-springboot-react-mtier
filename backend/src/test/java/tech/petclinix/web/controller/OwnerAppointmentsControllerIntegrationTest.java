package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.Appointment;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.OwnerAppointmentService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link OwnerAppointmentsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(OwnerAppointmentsController.class)
@Import(SecurityConfig.class)
class OwnerAppointmentsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OwnerAppointmentService appointmentService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a list of appointments for the authenticated owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void listReturnsOkWithAppointmentList() throws Exception {
        //arrange
        var appt = new Appointment(1L, 10L, 20L, LocalDateTime.of(2026, 5, 1, 9, 0));
        when(appointmentService.findAllByOwner(new Username("alice")))
                .thenReturn(List.of(appt));

        //act + assert
        mockMvc.perform(get("/owner/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].vetId").value(10))
                .andExpect(jsonPath("$[0].petId").value(20));
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void listReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/appointments"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void listReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/appointments"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with the created appointment when the request body is valid. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsOkWithCreatedAppointment() throws Exception {
        //arrange
        var startsAt = LocalDateTime.of(2026, 5, 1, 9, 0);
        var appt = new Appointment(1L, 10L, 20L, startsAt);
        when(appointmentService.persist(eq(new Username("alice")), any()))
                .thenReturn(appt);

        var body = """
                {"vetId":10,"petId":20,"startsAt":"2026-05-01T09:00:00"}
                """;

        //act + assert
        mockMvc.perform(post("/owner/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vetId").value(10))
                .andExpect(jsonPath("$.petId").value(20));
    }

    /** Returns 400 when required appointment fields are missing. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        //act + assert
        mockMvc.perform(post("/owner/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 204 when the appointment is successfully cancelled by its owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void cancelReturnsNoContentOnSuccess() throws Exception {
        //act + assert
        mockMvc.perform(delete("/owner/appointments/1"))
                .andExpect(status().isNoContent());
    }

    /** Returns 404 when the appointment does not belong to the authenticated owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void cancelReturnsNotFoundWhenAppointmentBelongsToAnotherOwner() throws Exception {
        //arrange
        doThrow(new NotFoundException("Appointment not found: 99"))
                .when(appointmentService).cancelByOwner(new Username("alice"), 99L);

        //act + assert
        mockMvc.perform(delete("/owner/appointments/99"))
                .andExpect(status().isNotFound());
    }

    /** Returns 403 when a VET tries to cancel an owner appointment. */
    @Test
    @WithMockUser(roles = "VET")
    void cancelReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(delete("/owner/appointments/1"))
                .andExpect(status().isForbidden());
    }
}
