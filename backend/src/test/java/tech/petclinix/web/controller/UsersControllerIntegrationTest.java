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
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.logic.service.UserType;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.mapper.UserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for user registration endpoint.
 * <p>
 * - Uses the real Spring context (controllers, services, repositories wired)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UsersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void register_success_returnsUserResponse() throws Exception {
        //arrange
        var requestJson = """
                {"username":"newuser","password":"secret123","type":"owner"}
                """;

        //act
        var result = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk()) // controller returns 200 OK with UserResponse
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);

        assertThat(node.has("id")).isTrue();
        assertThat(node.get("username").asText()).isEqualTo("newuser");

        // also verify persisted
        var saved = userJpaRepository.findByUsername("newuser");
        assertThat(saved).isPresent();
        assertThat(passwordEncoder.matches("secret123", saved.get().getPasswordHash())).isTrue();
        assertThat(UserMapper.getUserType(saved.get())).isEqualTo(UserType.OWNER);
    }

    @Test
    void register_conflictWhenUsernameExists_returns409() throws Exception {
        //arrange
        // seed existing user
        var encoded = passwordEncoder.encode("already");
        userJpaRepository.save(new OwnerEntity("taken", encoded));

        var requestJson = """
                {"username":"taken","password":"whatever","type":"owner"}
                """;

        //act + assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
    }
}
