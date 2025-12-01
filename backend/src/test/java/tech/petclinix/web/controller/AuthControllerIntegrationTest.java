package tech.petclinix.web.controller;

import tech.petclinix.logic.service.UserType;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.web.dto.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for authentication flow.
 *
 * Requirements:
 * - The application uses an in-memory DB (H2) for tests, or test properties override DB config.
 * - JwtUtil reads jwt.secret from properties; we provide a test-friendly base64 secret here.
 *
 * This test:
 * 1) seeds a user (username="user", password="password") using the real repository & password encoder
 * 2) POSTs /api/auth/login to obtain a JWT
 * 3) calls a protected endpoint with Authorization: Bearer <token> and verifies the response
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

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

        // seed a user: username = "user", password = "password"
        var encoded = passwordEncoder.encode("password");
        var entity = new OwnerEntity("user", encoded);
        userJpaRepository.save(entity);
    }

    @Test
    void loginAndAccessProtectedEndpoint_success() throws Exception {
        //arrange
        // 1) perform login
        String loginJson = """
            {"username":"user","password":"password"}
            """;

        //act
        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(loginBody);

        assertThat(node.has("token")).isTrue();
        String token = node.get("token").asText();
        assertThat(token).isNotBlank();

        //assert
        // 2) call protected endpoint with Authorization header
        var protectedResult = mockMvc.perform(get("/users/aboutme")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(protectedResult.getResponse().getContentAsString(), UserResponse.class);
        // expected response contains username (depends on your controller implementation)
        assertThat(userResponse.username()).isEqualTo("user");
        assertThat(userResponse.isOwner()).isTrue();
    }
}
