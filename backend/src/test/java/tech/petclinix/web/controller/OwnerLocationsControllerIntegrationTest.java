package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.BookableLocation;
import tech.petclinix.logic.service.LocationService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
    LocationService locationService;

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
        when(locationService.findAllBookable()).thenReturn(List.of(sampleBookableLocation()));

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
}
