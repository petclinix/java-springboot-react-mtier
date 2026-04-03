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
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetVisit;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.VetVisitService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link VetVisitsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(VetVisitsController.class)
@Import(SecurityConfig.class)
class VetVisitsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    VetVisitService vetVisitService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with visit details for the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void getReturnsOkWithVetVisit() throws Exception {
        //arrange
        var visit = new VetVisit(1L, "Healthy cat", "Give meds", "Rabies");
        when(vetVisitService.retrieveByVetAndId(new Username("drsmith"), 1L))
                .thenReturn(visit);

        //act + assert
        mockMvc.perform(get("/vet/visits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vetSummary").value("Healthy cat"))
                .andExpect(jsonPath("$.vaccination").value("Rabies"));
    }

    /** Returns 404 when the appointment belongs to another vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void getReturnsNotFoundWhenAppointmentBelongsToAnotherVet() throws Exception {
        //arrange
        when(vetVisitService.retrieveByVetAndId(new Username("drsmith"), 99L))
                .thenThrow(new NotFoundException("Appointment not found: 99"));

        //act + assert
        mockMvc.perform(get("/vet/visits/99"))
                .andExpect(status().isNotFound());
    }

    /** Returns 403 when the caller has the OWNER role instead of VET. */
    @Test
    @WithMockUser(roles = "OWNER")
    void getReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/vet/visits/1"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void getReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/vet/visits/1"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with updated visit details after a successful PUT. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void putReturnsOkWithUpdatedVetVisit() throws Exception {
        //arrange
        var visit = new VetVisit(1L, "All good", "No meds needed", "Distemper");
        when(vetVisitService.persist(eq(new Username("drsmith")), eq(1L), any()))
                .thenReturn(visit);

        var body = """
                {"vetSummary":"All good","ownerSummary":"No meds needed","vaccination":"Distemper"}
                """;

        //act + assert
        mockMvc.perform(put("/vet/visits/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vetSummary").value("All good"))
                .andExpect(jsonPath("$.vaccination").value("Distemper"));
    }

    /** Returns 404 when putting a visit for an appointment belonging to another vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void putReturnsNotFoundWhenAppointmentBelongsToAnotherVet() throws Exception {
        //arrange
        when(vetVisitService.persist(eq(new Username("drsmith")), eq(99L), any()))
                .thenThrow(new NotFoundException("Appointment not found: 99"));

        var body = """
                {"vetSummary":"All good","ownerSummary":"No meds","vaccination":"None"}
                """;

        //act + assert
        mockMvc.perform(put("/vet/visits/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    /** Returns 403 when an OWNER tries to put a vet visit. */
    @Test
    @WithMockUser(roles = "OWNER")
    void putReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(put("/vet/visits/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
