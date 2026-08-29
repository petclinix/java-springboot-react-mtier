package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.AvailableSlot;
import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.AvailabilityService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link OwnerLocationsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(OwnerLocationsController.class)
@Import(SecurityConfig.class)
class OwnerLocationsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AvailabilityService availabilityService;

    @MockBean
    JwtUtil jwtUtil;

    private BookableLocation sampleBookableLocation() {
        return new BookableLocation(1L, "PetClinix", "drsmith", "Europe/Vienna", null, null, null, null);
    }

    /** Returns 200 with a list of bookable locations for an authenticated owner. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAllReturnsOkWithBookableLocationList() throws Exception {
        //arrange
        when(availabilityService.findAllBookable()).thenReturn(List.of(sampleBookableLocation()));

        //act + assert
        mockMvc.perform(get("/owner/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("PetClinix"))
                .andExpect(jsonPath("$[0].vetUsername").value("drsmith"));
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void retrieveAllReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void retrieveAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with the list of available slots for the given location and date. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAvailableSlotsReturnsOkWithSlotList() throws Exception {
        //arrange
        var slot = new AvailableSlot(LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 30));
        when(availabilityService.findAvailableSlots(eq(1L), eq(LocalDate.of(2026, 9, 7))))
                .thenReturn(List.of(slot));

        //act + assert
        mockMvc.perform(get("/owner/locations/1/available-slots").param("date", "2026-09-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].startsAt").value("2026-09-07T09:00:00"))
                .andExpect(jsonPath("$[0].endsAt").value("2026-09-07T09:30:00"));
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void retrieveAvailableSlotsReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations/1/available-slots").param("date", "2026-09-07"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void retrieveAvailableSlotsReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations/1/available-slots").param("date", "2026-09-07"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 400 when the required date query parameter is missing. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAvailableSlotsReturnsBadRequestWhenDateMissing() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations/1/available-slots"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 400 when the date query parameter is malformed. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAvailableSlotsReturnsBadRequestWhenDateMalformed() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/locations/1/available-slots").param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 404 when the service reports the location does not exist. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAvailableSlotsReturnsNotFoundWhenLocationDoesNotExist() throws Exception {
        //arrange
        when(availabilityService.findAvailableSlots(eq(99L), any()))
                .thenThrow(new NotFoundException("Location not found: 99"));

        //act + assert
        mockMvc.perform(get("/owner/locations/99/available-slots").param("date", "2026-09-07"))
                .andExpect(status().isNotFound());
    }
}
