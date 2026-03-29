package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminUsersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private OwnerJpaRepository ownerJpaRepository;
    @Autowired
    private VetJpaRepository vetJpaRepository;
    @Autowired
    private PetJpaRepository petJpaRepository;
    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired
    private VisitJpaRepository visitJpaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        visitJpaRepository.deleteAllInBatch();
        appointmentJpaRepository.deleteAllInBatch();
        petJpaRepository.deleteAllInBatch();
        vetJpaRepository.deleteAll();
        ownerJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getReturnsAllUsers() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        ownerJpaRepository.save(new OwnerEntity("testowner", encoded));
        vetJpaRepository.save(new VetEntity("testvet", encoded));

        //act
        var result = mockMvc.perform(get("/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode nodes = objectMapper.readTree(body);

        assertThat(nodes.isArray()).isTrue();
        assertThat(nodes.size()).isEqualTo(2);

        boolean foundOwner = false;
        boolean foundVet = false;
        for (JsonNode node : nodes) {
            assertThat(node.get("id").asLong()).isGreaterThan(0);
            assertThat(node.get("active").asBoolean()).isTrue();
            String username = node.get("username").asText();
            String role = node.get("role").asText();
            if ("testowner".equals(username)) {
                assertThat(role).isEqualTo("OWNER");
                foundOwner = true;
            } else if ("testvet".equals(username)) {
                assertThat(role).isEqualTo("VET");
                foundVet = true;
            }
        }
        assertThat(foundOwner).isTrue();
        assertThat(foundVet).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deactivateSetUserInactive() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("targetowner", encoded));

        //act
        var result = mockMvc.perform(put("/admin/users/" + owner.getId() + "/deactivate"))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        assertThat(node.get("id").asLong()).isEqualTo(owner.getId());
        assertThat(node.get("username").asText()).isEqualTo("targetowner");
        assertThat(node.get("role").asText()).isEqualTo("OWNER");
        assertThat(node.get("active").asBoolean()).isFalse();

        var saved = userJpaRepository.findById(owner.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().isActive()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deactivatedUserCannotLogin() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("lockedout", encoded));

        //deactivate the user
        mockMvc.perform(put("/admin/users/" + owner.getId() + "/deactivate"))
                .andExpect(status().isOk());

        var loginJson = """
                {"username":"lockedout","password":"secret"}
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deactivateReturnsNotFoundForMissingUser() throws Exception {
        //arrange
        long nonExistentId = 999999L;

        //act + assert
        mockMvc.perform(put("/admin/users/" + nonExistentId + "/deactivate"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void activateReactivatesDeactivatedUser() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("reactivated", encoded));

        //deactivate first
        mockMvc.perform(put("/admin/users/" + owner.getId() + "/deactivate"))
                .andExpect(status().isOk());

        //act
        var result = mockMvc.perform(put("/admin/users/" + owner.getId() + "/activate"))
                .andExpect(status().isOk())
                .andReturn();

        //assert response has active=true
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        assertThat(node.get("id").asLong()).isEqualTo(owner.getId());
        assertThat(node.get("username").asText()).isEqualTo("reactivated");
        assertThat(node.get("role").asText()).isEqualTo("OWNER");
        assertThat(node.get("active").asBoolean()).isTrue();

        //assert user can log in again
        var loginJson = """
                {"username":"reactivated","password":"secret"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void getRequiresAdminRole() throws Exception {
        //arrange
        // no authentication provided

        //act + assert
        mockMvc.perform(get("/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
