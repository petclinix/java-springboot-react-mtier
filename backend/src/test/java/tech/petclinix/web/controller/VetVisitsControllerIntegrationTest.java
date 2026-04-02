package tech.petclinix.web.controller;

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
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository.Specifications;
import tech.petclinix.web.dto.VetVisitRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VetVisitsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerJpaRepository ownerJpaRepository;
    @Autowired private PetJpaRepository petJpaRepository;
    @Autowired private VetJpaRepository vetJpaRepository;
    @Autowired private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired private VisitJpaRepository visitJpaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        visitJpaRepository.deleteAllInBatch();
        appointmentJpaRepository.deleteAllInBatch();
        petJpaRepository.deleteAllInBatch();
        vetJpaRepository.deleteAll();   // cascades to LocationEntity
        ownerJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void getReturnsExistingVisit() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity appointment = appointmentJpaRepository.save(
                new AppointmentEntity(testvet, pet, LocalDateTime.now().plusDays(1)));
        VisitEntity visit = new VisitEntity(appointment);
        visit.setVetSummary("existing notes");
        visit.setVaccination("rabies");
        visitJpaRepository.save(visit);

        //act + assert
        mockMvc.perform(get("/vet/visits/" + appointment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vetSummary").value("existing notes"))
                .andExpect(jsonPath("$.vaccination").value("rabies"));
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void putSavesVisit() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity appointment = appointmentJpaRepository.save(
                new AppointmentEntity(testvet, pet, LocalDateTime.now().plusDays(1)));

        var requestJson = objectMapper.writeValueAsString(new VetVisitRequest("notes", null, "rabies"));

        //act
        mockMvc.perform(put("/vet/visits/" + appointment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vetSummary").value("notes"))
                .andExpect(jsonPath("$.vaccination").value("rabies"));

        //assert
        var saved = visitJpaRepository.findOne(Specifications.byAppointment(appointment));
        assertThat(saved).isPresent();
        assertThat(saved.get().getVetSummary()).isEqualTo("notes");
        assertThat(saved.get().getVaccination()).isEqualTo("rabies");
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void getReturnsNotFoundForOtherVetsAppointment() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        vetJpaRepository.save(new VetEntity("testvet", encoded));
        VetEntity othervet = vetJpaRepository.save(new VetEntity("othervet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity theirAppointment = appointmentJpaRepository.save(
                new AppointmentEntity(othervet, pet, LocalDateTime.now().plusDays(1)));

        //act + assert
        mockMvc.perform(get("/vet/visits/" + theirAppointment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void putSavesOwnerSummary() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity appointment = appointmentJpaRepository.save(
                new AppointmentEntity(testvet, pet, LocalDateTime.now().plusDays(1)));

        var requestJson = objectMapper.writeValueAsString(new VetVisitRequest("vet notes", "owner notes", "rabies"));

        //act
        mockMvc.perform(put("/vet/visits/" + appointment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vetSummary").value("vet notes"))
                .andExpect(jsonPath("$.ownerSummary").value("owner notes"))
                .andExpect(jsonPath("$.vaccination").value("rabies"));

        //assert
        var saved = visitJpaRepository.findOne(Specifications.byAppointment(appointment));
        assertThat(saved).isPresent();
        assertThat(saved.get().getVetSummary()).isEqualTo("vet notes");
        assertThat(saved.get().getOwnerSummary()).isEqualTo("owner notes");
        assertThat(saved.get().getVaccination()).isEqualTo("rabies");
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void putReturnsNotFoundForOtherVetsAppointment() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        vetJpaRepository.save(new VetEntity("testvet", encoded));
        VetEntity otherVet = vetJpaRepository.save(new VetEntity("othervet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity theirAppointment = appointmentJpaRepository.save(
                new AppointmentEntity(otherVet, pet, LocalDateTime.now().plusDays(1)));

        var requestJson = objectMapper.writeValueAsString(new VetVisitRequest("notes", null, "rabies"));

        //act + assert
        mockMvc.perform(put("/vet/visits/" + theirAppointment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }
}
