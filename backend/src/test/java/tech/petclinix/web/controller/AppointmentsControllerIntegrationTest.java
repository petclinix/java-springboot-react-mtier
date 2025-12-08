package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.*;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository.Specifications;
import tech.petclinix.web.dto.AppointmentRequest;

import java.time.LocalDateTime;

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
public class AppointmentsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerJpaRepository ownerJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PetJpaRepository petJpaRepository;

    @Autowired
    private VetJpaRepository vetJpaRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        appointmentJpaRepository.deleteAll();
        vetJpaRepository.deleteAll();

        petJpaRepository.deleteAll();
        ownerJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void post_creates_new_appointment() throws Exception {
        //arrange
        // seed existing user
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        assertThat(testuser.getId()).isNotNull();

        PetEntity pet = petJpaRepository.save(new PetEntity("pet", testuser));
        assertThat(pet.getId()).isNotNull();

        VetEntity vet = vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));
        assertThat(vet.getId()).isNotNull();

        var requestJson = objectMapper.writeValueAsString(new AppointmentRequest(vet.getId(), pet.getId(), LocalDateTime.now()));

        //act
        var result = mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode appointmentNode = objectMapper.readTree(body);

        assertThat(appointmentNode.has("id")).isTrue();

        // also verify persisted
        var saved = appointmentJpaRepository.findOne(Specifications.byVet(vet).and(Specifications.byPet(pet)));
        assertThat(saved).isPresent();

    }
}
