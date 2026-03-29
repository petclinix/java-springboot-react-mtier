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
import tech.petclinix.persistence.entity.VisitEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OwnerPetVisitsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerJpaRepository ownerJpaRepository;
    @Autowired private PetJpaRepository petJpaRepository;
    @Autowired private VetJpaRepository vetJpaRepository;
    @Autowired private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired private VisitJpaRepository visitJpaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        visitJpaRepository.deleteAllInBatch();
        appointmentJpaRepository.deleteAllInBatch();
        petJpaRepository.deleteAllInBatch();
        vetJpaRepository.deleteAll();
        ownerJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testowner", roles = {"OWNER"})
    void getReturnsVisitsForOwnPet() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("testowner", encoded));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vetuser", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));

        LocalDateTime startsAt1 = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime startsAt2 = LocalDateTime.now().plusDays(2).withNano(0);

        AppointmentEntity appt1 = appointmentJpaRepository.save(new AppointmentEntity(vet, pet, startsAt1));
        AppointmentEntity appt2 = appointmentJpaRepository.save(new AppointmentEntity(vet, pet, startsAt2));

        VisitEntity visit1 = new VisitEntity(appt1);
        visit1.setOwnerSummary("owner notes 1");
        visit1.setVaccination("rabies");
        visitJpaRepository.save(visit1);

        VisitEntity visit2 = new VisitEntity(appt2);
        visit2.setOwnerSummary("owner notes 2");
        visit2.setVaccination("distemper");
        visitJpaRepository.save(visit2);

        //act + assert
        mockMvc.perform(get("/owner/pets/" + pet.getId() + "/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ownerSummary").value("owner notes 1"))
                .andExpect(jsonPath("$[0].vaccination").value("rabies"))
                .andExpect(jsonPath("$[0].vetUsername").value("vetuser"))
                .andExpect(jsonPath("$[0].startsAt").isNotEmpty())
                .andExpect(jsonPath("$[1].ownerSummary").value("owner notes 2"))
                .andExpect(jsonPath("$[1].vaccination").value("distemper"))
                .andExpect(jsonPath("$[1].vetUsername").value("vetuser"))
                .andExpect(jsonPath("$[1].startsAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testowner", roles = {"OWNER"})
    void getReturnsEmptyListWhenNoVisits() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("testowner", encoded));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vetuser", encoded));
        PetEntity pet = petJpaRepository.save(new PetEntity("fluffy", owner));

        appointmentJpaRepository.save(new AppointmentEntity(vet, pet, LocalDateTime.now().plusDays(1)));

        //act + assert
        mockMvc.perform(get("/owner/pets/" + pet.getId() + "/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testowner", roles = {"OWNER"})
    void getReturnsNotFoundForOtherOwnersPet() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        ownerJpaRepository.save(new OwnerEntity("testowner", encoded));
        OwnerEntity otherOwner = ownerJpaRepository.save(new OwnerEntity("otherowner", encoded));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vetuser", encoded));
        PetEntity theirPet = petJpaRepository.save(new PetEntity("theirpet", otherOwner));

        AppointmentEntity appt = appointmentJpaRepository.save(
                new AppointmentEntity(vet, theirPet, LocalDateTime.now().plusDays(1)));
        VisitEntity visit = new VisitEntity(appt);
        visit.setOwnerSummary("their notes");
        visitJpaRepository.save(visit);

        //act + assert
        mockMvc.perform(get("/owner/pets/" + theirPet.getId() + "/visits"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testowner", roles = {"OWNER"})
    void getReturnsOnlyVisitsForRequestedPet() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner = ownerJpaRepository.save(new OwnerEntity("testowner", encoded));
        VetEntity vet = vetJpaRepository.save(new VetEntity("vetuser", encoded));
        PetEntity pet1 = petJpaRepository.save(new PetEntity("fluffy", owner));
        PetEntity pet2 = petJpaRepository.save(new PetEntity("rex", owner));

        AppointmentEntity appt1 = appointmentJpaRepository.save(
                new AppointmentEntity(vet, pet1, LocalDateTime.now().plusDays(1)));
        AppointmentEntity appt2 = appointmentJpaRepository.save(
                new AppointmentEntity(vet, pet2, LocalDateTime.now().plusDays(2)));

        VisitEntity visit1 = new VisitEntity(appt1);
        visit1.setOwnerSummary("notes for fluffy");
        visitJpaRepository.save(visit1);

        VisitEntity visit2 = new VisitEntity(appt2);
        visit2.setOwnerSummary("notes for rex");
        visitJpaRepository.save(visit2);

        //act + assert
        mockMvc.perform(get("/owner/pets/" + pet1.getId() + "/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerSummary").value("notes for fluffy"));
    }
}
