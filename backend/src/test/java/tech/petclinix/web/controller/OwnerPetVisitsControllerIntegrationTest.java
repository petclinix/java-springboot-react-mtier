package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.OwnerVisit;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.PetVisitService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link OwnerPetVisitsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(OwnerPetVisitsController.class)
@Import(SecurityConfig.class)
class OwnerPetVisitsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PetVisitService petVisitService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a list of visits for the authenticated owner's pet. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void listReturnsOkWithVisitList() throws Exception {
        //arrange
        var visit = new OwnerVisit(1L, "Take meds", "Rabies", "drsmith",
                LocalDateTime.of(2026, 5, 1, 9, 0));
        when(petVisitService.findAllVisitsByOwnerAndPet(new Username("alice"), 10L))
                .thenReturn(List.of(visit));

        //act + assert
        mockMvc.perform(get("/owner/pets/10/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].vaccination").value("Rabies"))
                .andExpect(jsonPath("$[0].vetUsername").value("drsmith"));
    }

    /** Returns 404 when the pet belongs to another owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void listReturnsNotFoundWhenPetBelongsToAnotherOwner() throws Exception {
        //arrange
        when(petVisitService.findAllVisitsByOwnerAndPet(new Username("alice"), 99L))
                .thenThrow(new NotFoundException("Pet not found: 99"));

        //act + assert
        mockMvc.perform(get("/owner/pets/99/visits"))
                .andExpect(status().isNotFound());
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void listReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/pets/10/visits"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void listReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/owner/pets/10/visits"))
                .andExpect(status().isUnauthorized());
    }
}
