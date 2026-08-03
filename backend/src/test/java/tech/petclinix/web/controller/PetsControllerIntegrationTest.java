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
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.service.PetService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import tech.petclinix.logic.domain.PetData;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
                .thenReturn(List.of(new Pet(1L, "Fluffy", null, "Labrador", null, null, null, null, true)));

        //act + assert
        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Fluffy"))
                .andExpect(jsonPath("$[0].breed").value("Labrador"))
                .andExpect(jsonPath("$[0].active").value(true));
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

    /** Returns 200 with the created pet, including base64 picture fields, for the authenticated owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsOkWithCreatedPet() throws Exception {
        //arrange
        byte[] pictureBytes = "fake-image-bytes".getBytes();
        when(petService.persist(eq(new Username("alice")), any(PetData.class)))
                .thenReturn(new Pet(2L, "Fluffy", "CAT", "Siamese", "FEMALE", null, pictureBytes, "image/png", true));

        var body = """
                {"name":"Fluffy","species":"CAT","breed":"Siamese","gender":"FEMALE","picture":"%s","pictureContentType":"image/png"}
                """.formatted(java.util.Base64.getEncoder().encodeToString(pictureBytes));

        //act + assert
        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Fluffy"))
                .andExpect(jsonPath("$.breed").value("Siamese"))
                .andExpect(jsonPath("$.picture").value(java.util.Base64.getEncoder().encodeToString(pictureBytes)))
                .andExpect(jsonPath("$.pictureContentType").value("image/png"));
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

    /** Returns 400 when the request body is missing the required username field. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        //act + assert
        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 200 with the updated pet when the update succeeds. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void updateReturnsOkWithUpdatedPet() throws Exception {
        //arrange
        when(petService.update(eq(new Username("alice")), eq(1L), any(PetData.class)))
                .thenReturn(new Pet(1L, "Fluffy II", "CAT", "Siamese", "FEMALE", null, null, null, true));

        var body = """
                {"name":"Fluffy II","species":"CAT","breed":"Siamese","gender":"FEMALE"}
                """;

        //act + assert
        mockMvc.perform(put("/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fluffy II"));
    }

    /** Returns 403 when a VET tries to update a pet. */
    @Test
    @WithMockUser(roles = "VET")
    void updateReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(put("/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fluffy\"}"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when updating a pet without authentication. */
    @Test
    void updateReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(put("/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fluffy\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 400 when the update request body is missing the required name field. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void updateReturnsBadRequestWhenNameIsMissing() throws Exception {
        //act + assert
        mockMvc.perform(put("/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /** Returns 404 when updating a pet that belongs to another owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void updateReturnsNotFoundWhenPetBelongsToAnotherOwner() throws Exception {
        //arrange
        when(petService.update(eq(new Username("alice")), eq(99L), any(PetData.class)))
                .thenThrow(new NotFoundException("Pet not found: 99"));

        //act + assert
        mockMvc.perform(put("/pets/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fluffy\"}"))
                .andExpect(status().isNotFound());
    }

    /** Returns 204 when the authenticated owner deactivates their own pet. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void deleteReturnsNoContentWhenPetBelongsToOwner() throws Exception {
        //act + assert
        mockMvc.perform(delete("/pets/1"))
                .andExpect(status().isNoContent());
    }

    /** Returns 403 when a VET tries to delete a pet. */
    @Test
    @WithMockUser(roles = "VET")
    void deleteReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(delete("/pets/1"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when deleting a pet without authentication. */
    @Test
    void deleteReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(delete("/pets/1"))
                .andExpect(status().isUnauthorized());
    }

    /** Returns 404 when deleting a pet that belongs to another owner. */
    @Test
    @WithMockUser(username = "alice", roles = "OWNER")
    void deleteReturnsNotFoundWhenPetBelongsToAnotherOwner() throws Exception {
        //arrange
        doThrow(new NotFoundException("Pet not found: 99"))
                .when(petService).deactivate(new Username("alice"), 99L);

        //act + assert
        mockMvc.perform(delete("/pets/99"))
                .andExpect(status().isNotFound());
    }
}
