package tech.petclinix.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import tech.petclinix.persistence.entity.AppointmentEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void getReturnsAppointmentsForAuthenticated_owner() throws Exception {
        // arrange
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        OwnerEntity otheruser = ownerJpaRepository.save(new OwnerEntity("otheruser", encoded));

        PetEntity myPet = petJpaRepository.save(new PetEntity("myPet", testuser));
        PetEntity theirPet = petJpaRepository.save(new PetEntity("theirPet", otheruser));

        VetEntity vet = vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));

        appointmentJpaRepository.save(new AppointmentEntity(vet, myPet, LocalDateTime.now().plusDays(1)));
        appointmentJpaRepository.save(new AppointmentEntity(vet, myPet, LocalDateTime.now().plusDays(2)));
        appointmentJpaRepository.save(new AppointmentEntity(vet, theirPet, LocalDateTime.now().plusDays(3)));

        // act & assert
        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void postCreatesNewAppointment() throws Exception {
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

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void getReturnsAppointmentFields() throws Exception {
        // arrange
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", testuser));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));
        var startsAt = LocalDateTime.now().plusDays(1).withNano(0);
        appointmentJpaRepository.save(new AppointmentEntity(vet, pet, startsAt));

        // act & assert
        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].vetId").value(vet.getId()))
                .andExpect(jsonPath("$[0].petId").value(pet.getId()))
                .andExpect(jsonPath("$[0].startsAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void deleteCancelsOwnAppointment() throws Exception {
        // arrange
        var encoded = passwordEncoder.encode("already");
        OwnerEntity testuser = ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", testuser));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));
        AppointmentEntity appointment = appointmentJpaRepository.save(
                new AppointmentEntity(vet, pet, LocalDateTime.now().plusDays(1)));

        // act
        mockMvc.perform(delete("/appointments/" + appointment.getId()))
                .andExpect(status().isNoContent());

        // assert: removed from DB
        assertThat(appointmentJpaRepository.findById(appointment.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"OWNER"})
    void deleteReturnsNotFoundForOtherOwnersAppointment() throws Exception {
        // arrange
        var encoded = passwordEncoder.encode("already");
        ownerJpaRepository.save(new OwnerEntity("testuser", encoded));
        OwnerEntity otheruser = ownerJpaRepository.save(new OwnerEntity("otheruser", encoded));
        PetEntity theirPet = petJpaRepository.save(new PetEntity("theirpet", otheruser));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vet", passwordEncoder.encode("secret")));
        AppointmentEntity theirAppointment = appointmentJpaRepository.save(
                new AppointmentEntity(vet, theirPet, LocalDateTime.now().plusDays(1)));

        // act & assert
        mockMvc.perform(delete("/appointments/" + theirAppointment.getId()))
                .andExpect(status().isNotFound());

        // appointment is still in DB
        assertThat(appointmentJpaRepository.findById(theirAppointment.getId())).isPresent();
    }
}
