package tech.petclinix.web.controller;

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
@TestPropertySource(properties = {
        // Base64-encoded secret long enough for HS256/HS512; adjust depending on your JwtUtil expectations.
        // This is a 32-byte base64 (suitable for HS256). If your JwtUtil uses HS512, use a 64-byte base64.
        "jwt.secret=Rm9vQmFyQmF6MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY=",
        "jwt.expirationMs=3600000",
        // ensure using in-memory DB defaults (if you use H2 already, this is optional)
        "spring.datasource.url=jdbc:h2:mem:itdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
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
        var entity = new UserEntity("user", encoded);
        userJpaRepository.save(entity);
    }

    @Test
    void loginAndAccessProtectedEndpoint_success() throws Exception {
        // 1) perform login
        String loginJson = """
            {"username":"user","password":"password"}
            """;

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

        // 2) call protected endpoint with Authorization header
        var protectedResult = mockMvc.perform(get("/protected/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String protectedBody = protectedResult.getResponse().getContentAsString();
        // expected response contains username (depends on your controller implementation)
        assertThat(protectedBody).contains("Hello");
        assertThat(protectedBody).contains("user");
    }
}
