package tech.petclinix.web.controller;

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
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.persistence.jpa.VisitJpaRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminStatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OwnerJpaRepository ownerJpaRepository;
    @Autowired
    private VetJpaRepository vetJpaRepository;
    @Autowired
    private PetJpaRepository petJpaRepository;
    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired
    private VisitJpaRepository visitJpaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getReturnsCorrectCounts() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("secret");
        OwnerEntity owner1 = ownerJpaRepository.save(new OwnerEntity("owner1", encoded));
        OwnerEntity owner2 = ownerJpaRepository.save(new OwnerEntity("owner2", encoded));
        VetEntity vet1 = vetJpaRepository.save(new VetEntity("vet1", encoded));
        VetEntity vet2 = vetJpaRepository.save(new VetEntity("vet2", encoded));
        PetEntity pet1 = petJpaRepository.save(new PetEntity("pet1", owner1));
        PetEntity pet2 = petJpaRepository.save(new PetEntity("pet2", owner1));
        PetEntity pet3 = petJpaRepository.save(new PetEntity("pet3", owner2));
        appointmentJpaRepository.save(new AppointmentEntity(vet1, pet1, LocalDateTime.now().plusDays(1)));
        appointmentJpaRepository.save(new AppointmentEntity(vet1, pet2, LocalDateTime.now().plusDays(2)));
        appointmentJpaRepository.save(new AppointmentEntity(vet1, pet3, LocalDateTime.now().plusDays(3)));
        appointmentJpaRepository.save(new AppointmentEntity(vet2, pet1, LocalDateTime.now().plusDays(4)));

        //act + assert
        mockMvc.perform(get("/admin/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwners").value(2))
                .andExpect(jsonPath("$.totalVets").value(2))
                .andExpect(jsonPath("$.totalPets").value(3))
                .andExpect(jsonPath("$.totalAppointments").value(4))
                .andExpect(jsonPath("$.appointmentsPerVet[0].vetUsername").value("vet1"))
                .andExpect(jsonPath("$.appointmentsPerVet[0].count").value(3))
                .andExpect(jsonPath("$.appointmentsPerVet[1].vetUsername").value("vet2"))
                .andExpect(jsonPath("$.appointmentsPerVet[1].count").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getReturnsEmptyStatsWhenNoData() throws Exception {
        //arrange
        // no seed data

        //act + assert
        mockMvc.perform(get("/admin/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwners").value(0))
                .andExpect(jsonPath("$.totalVets").value(0))
                .andExpect(jsonPath("$.totalPets").value(0))
                .andExpect(jsonPath("$.totalAppointments").value(0))
                .andExpect(jsonPath("$.appointmentsPerVet").isArray())
                .andExpect(jsonPath("$.appointmentsPerVet").isEmpty());
    }

    @Test
    void getRequiresAdminRole() throws Exception {
        //arrange
        // no authentication provided

        //act + assert
        mockMvc.perform(get("/admin/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
