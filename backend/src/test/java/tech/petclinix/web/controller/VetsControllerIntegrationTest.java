package tech.petclinix.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.domain.Vet;
import tech.petclinix.logic.service.VetService;
import tech.petclinix.security.config.SecurityConfig;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link VetsController}.
 *
 * Verifies the HTTP contract: JSON serialisation/deserialisation, HTTP status codes,
 * and security enforcement. The service layer is mocked — business logic is not tested here.
 */
@WebMvcTest(VetsController.class)
@Import(SecurityConfig.class)
class VetsControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VetService vetService;

    @MockBean
    JwtUtil jwtUtil;

    /** Returns 200 with a JSON array of vets for an authenticated owner. */
    @Test
    @WithMockUser(roles = "OWNER")
    void retrieveAllReturnsOkWithVetList() throws Exception {
        //arrange
        when(vetService.findAll()).thenReturn(List.of(new Vet(1L, "drsmith")));

        //act + assert
        mockMvc.perform(get("/vets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("drsmith"));
    }

    /** Returns 403 when the caller has the VET role instead of OWNER. */
    @Test
    @WithMockUser(roles = "VET")
    void retrieveAllReturnsForbiddenForVetRole() throws Exception {
        //act + assert
        mockMvc.perform(get("/vets"))
                .andExpect(status().isForbidden());
    }

    /** Returns 401 when no authentication is provided. */
    @Test
    void retrieveAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        //act + assert
        mockMvc.perform(get("/vets"))
                .andExpect(status().isUnauthorized());
    }
}
