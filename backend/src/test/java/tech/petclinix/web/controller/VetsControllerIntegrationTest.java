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
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for vets endpoint.
 * <p>
 * - Uses the real Spring context (controllers, services, repositories wired)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class VetsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VetJpaRepository vetJpaRepository;

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
    void retrieve_all_vets() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));

        //act
        var result = mockMvc.perform(get("/vets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);

        assertThat(node.isArray()).isTrue();
        JsonNode vetNode = node.elements().next();

        assertThat(vetNode.has("id")).isTrue();
        assertThat(vetNode.get("name").asText()).isEqualTo("vet");
    }

    @Test
    void retrieve_all_vets_is_notPublic() throws Exception {
        //arrange
        vetJpaRepository.save(new VetEntity("publicvet", passwordEncoder.encode("secret")));

        //act + assert
        mockMvc.perform(get("/vets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}
