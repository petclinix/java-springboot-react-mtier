package tech.petclinix.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VetAppointmentsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerJpaRepository ownerJpaRepository;
    @Autowired private PetJpaRepository petJpaRepository;
    @Autowired private VetJpaRepository vetJpaRepository;
    @Autowired private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        appointmentJpaRepository.deleteAllInBatch();
        petJpaRepository.deleteAllInBatch();
        vetJpaRepository.deleteAll();
        ownerJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void getReturnsOnlyOwnAppointments() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        VetEntity othervet = vetJpaRepository.save(new VetEntity("othervet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet1 = petJpaRepository.save(new PetEntity("pet1", owner));
        PetEntity pet2 = petJpaRepository.save(new PetEntity("pet2", owner));

        appointmentJpaRepository.save(new AppointmentEntity(testvet, pet1, LocalDateTime.now().plusDays(1)));
        appointmentJpaRepository.save(new AppointmentEntity(testvet, pet2, LocalDateTime.now().plusDays(2)));
        appointmentJpaRepository.save(new AppointmentEntity(othervet, pet1, LocalDateTime.now().plusDays(3)));

        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void getReturnsAppointmentFields() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        appointmentJpaRepository.save(new AppointmentEntity(testvet, pet, LocalDateTime.now().plusDays(1).withNano(0)));

        //act + assert
        mockMvc.perform(get("/vet/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].petId").value(pet.getId()))
                .andExpect(jsonPath("$[0].petName").value("fluffy"))
                .andExpect(jsonPath("$[0].ownerUsername").value("owner"))
                .andExpect(jsonPath("$[0].startsAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void deleteCancelsOwnAppointment() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        VetEntity testvet = vetJpaRepository.save(new VetEntity("testvet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity appointment = appointmentJpaRepository.save(
                new AppointmentEntity(testvet, pet, LocalDateTime.now().plusDays(1)));

        //act
        mockMvc.perform(delete("/vet/appointments/" + appointment.getId()))
                .andExpect(status().isNoContent());

        //assert
        assertThat(appointmentJpaRepository.findById(appointment.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "testvet", roles = {"VET"})
    void deleteReturnsNotFoundForOtherVetsAppointment() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        vetJpaRepository.save(new VetEntity("testvet", encoded));
        VetEntity othervet = vetJpaRepository.save(new VetEntity("othervet", encoded));
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("owner", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));
        AppointmentEntity theirAppointment = appointmentJpaRepository.save(
                new AppointmentEntity(othervet, pet, LocalDateTime.now().plusDays(1)));

        //act
        mockMvc.perform(delete("/vet/appointments/" + theirAppointment.getId()))
                .andExpect(status().isNotFound());

        //assert
        assertThat(appointmentJpaRepository.findById(theirAppointment.getId())).isPresent();
    }
}
