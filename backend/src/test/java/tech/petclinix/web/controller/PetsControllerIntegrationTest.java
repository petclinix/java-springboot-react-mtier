package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.service.UserType;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for pets administration endpoint.
 * <p>
 * - Uses the real Spring context (controllers, services, repositories wired)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class PetsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PetJpaRepository petJpaRepository;

    @Autowired
    private OwnerJpaRepository ownerJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void retrieve_all_pets() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded, UserType.OWNER));
        petJpaRepository.save(new PetEntity("kittycat", testuser));

        //act
        var result = mockMvc.perform(get("/pets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // controller returns 200 OK with PetResponse
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);

        assertThat(node.isArray()).isTrue();
        JsonNode petNode = node.elements().next();

        assertThat(petNode.has("id")).isTrue();
        assertThat(petNode.get("name").asText()).isEqualTo("kittycat");

        // also verify persisted
        var saved = petJpaRepository.findByName("kittycat");
        assertThat(saved).isPresent();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void post_creates_new_pet() throws Exception {
        //arrange
        // seed existing user
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded, UserType.OWNER));
        assertThat(testuser.getId()).isNotNull();

        var requestJson = """
                {"name":"tom"}
                """;

        //act
        var result = mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode petNode = objectMapper.readTree(body);

        assertThat(petNode.has("id")).isTrue();
        assertThat(petNode.get("name").asText()).isEqualTo("tom");

        // also verify persisted
        var saved = petJpaRepository.findByName("tom");
        assertThat(saved).isPresent();

    }
}
