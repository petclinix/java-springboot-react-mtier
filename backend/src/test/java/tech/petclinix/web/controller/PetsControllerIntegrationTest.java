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
import tech.petclinix.logic.domain.Pet;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link PetsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(PetsController.class)
@Import(SecurityConfig.class)
class PetsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PetService petService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a JSON array of pets for the authenticated owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void retrieveAllReturnsOkWithPetList() throws Exception {
        //arrange
        when(petService.findAllByOwner(new Username("alice")))
                .thenReturn(List.of(new Pet(1L, "Fluffy", null, null, null)));

        //act + assert
        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Fluffy"));
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void retrieveAllReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/pets"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void retrieveAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/pets"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 200 with the created pet for the authenticated owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsOkWithCreatedPet() throws Exception {
        //arrange
        when(petService.persist(new Username("alice"), "Fluffy"))
                .thenReturn(new Pet(2L, "Fluffy", null, null, null));

        var body = """
                {"name":"Fluffy"}
                """;

        //act + assert
        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Fluffy"));
    }

    /** Returns 403 when a VET tries to create a pet. */
    @Test
    @WithMockUser(roles = "VET")
    void createReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fluffy\"}"))
                .andExpect(status().isForbidden());
    }

    /** Returns 400 when the request body is missing the required name field. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        //act + assert
        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
