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
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link LocationsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(LocationsController.class)
@Import(SecurityConfig.class)
class LocationsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LocationService locationService;

    @MockBean
    JwtUtil jwtUtil;

    private Location sampleLocation() {
        return new Location(1L, "PetClinix", "Europe/Vienna", List.of(), List.of());
    }

    /** Returns 200 with a list of locations for an authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void retrieveAllReturnsOkWithLocationList() throws Exception {
        //arrange
        when(locationService.findAllByVet(new Username("drsmith")))
                .thenReturn(List.of(sampleLocation()));

        //act + assert
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("PetClinix"));
    }

    /** Returns 403 when the caller has the OWNER role instead of VET. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAllReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/locations"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void retrieveAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/locations"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with a single location when it belongs to the authenticated vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void retrieveReturnsOkWithLocation() throws Exception {
        //arrange
        when(locationService.findByVetAndId(new Username("drsmith"), 1L))
                .thenReturn(sampleLocation());

        //act + assert
        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("PetClinix"));
    }

    /** Returns 404 when the location belongs to another vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void retrieveReturnsNotFoundWhenLocationBelongsToAnotherVet() throws Exception {
        //arrange
        when(locationService.findByVetAndId(new Username("drsmith"), 99L))
                .thenThrow(new NotFoundException("Location not found: 99"));

        //act + assert
        mockMvc.perform(get("/locations/99"))
                .andExpect(status().isNotFound());
    }

    /** Returns 200 with the created location when the body is valid. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void createReturnsOkWithCreatedLocation() throws Exception {
        //arrange
        var location = sampleLocation();
        when(locationService.persist(eq(new Username("drsmith")), any()))
                .thenReturn(location);

        var body = objectMapper.writeValueAsString(location);

        //act + assert
        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PetClinix"));
    }

    /** Returns 403 when an OWNER tries to create a location. */
    @Test
    @WithMockUser(roles = "OWNER")
    void createReturnsForbiddenForOwnerRole() throws Exception {
        //act + assert
        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    /** Returns 200 with the updated location when the update succeeds. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void updateReturnsOkWithUpdatedLocation() throws Exception {
        //arrange
        var location = new Location(1L, "Updated Clinic", "Europe/Berlin", List.of(), List.of());
        when(locationService.update(eq(new Username("drsmith")), eq(1L), any()))
                .thenReturn(location);

        var body = objectMapper.writeValueAsString(location);

        //act + assert
        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Clinic"));
    }

    /** Returns 204 when the authenticated vet deletes their own location. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void deleteReturnsNoContentWhenLocationBelongsToVet() throws Exception {
        //act + assert
        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isNoContent());
    }

    /** Returns 404 when deleting a location that belongs to another vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void deleteReturnsNotFoundWhenLocationBelongsToAnotherVet() throws Exception {
        //arrange
        doThrow(new NotFoundException("Location not found: 99"))
                .when(locationService).delete(new Username("drsmith"), 99L);

        //act + assert
        mockMvc.perform(delete("/locations/99"))
                .andExpect(status().isNotFound());
    }

    /** Returns 404 when updating a location that belongs to another vet. */
    @Test
    @WithMockUser(username = "drsmith", roles = "VET")
    void updateReturnsNotFoundWhenLocationBelongsToAnotherVet() throws Exception {
        //arrange
        when(locationService.update(eq(new Username("drsmith")), eq(99L), any()))
                .thenThrow(new NotFoundException("Location not found: 99"));

        var body = objectMapper.writeValueAsString(sampleLocation());

        //act + assert
        mockMvc.perform(put("/locations/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
