package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Provide a strong base64 key that matches algorithm requirements (HS512).
        "jwt.secret=FaoYp2rCsi4aCR0qtSGnC2b76D5bvJtdJTJrpbrudXVUyQKbaCt2/OSDxvQb2S3JQf7gK0c6Zq2l1wS6fWwYxv6q8rJmH9n0P4tV8Y1bC3D4e5F6G7H8I9J0K==",
        "jwt.expirationMs=3600000"
})
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAndAccessProtectedEndpoint_success() throws Exception {
        //arrange+act

        // 1) login
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
        String token = node.get("token").asText();

        assertThat(token).isNotBlank();

        // 2) call protected endpoint with Authorization header
        var protectedResult = mockMvc.perform(get("/protected/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String protectedBody = protectedResult.getResponse().getContentAsString();
        assertThat(protectedBody).contains("Hello");
        assertThat(protectedBody).contains("user");
    }
}
