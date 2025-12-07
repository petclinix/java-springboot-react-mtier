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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.web.dto.LocationResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningPeriodResponse;
import tech.petclinix.web.dto.LocationResponse.OpeningExceptionResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for locoations administration endpoint.
 * <p>
 * - Uses the real Spring context (controllers, services, repositories wired)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class LocationsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocationJpaRepository locationJpaRepository;

    @Autowired
    private VetJpaRepository vetJpaRepository;

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
    @WithMockUser(username = "testuser", roles = {"VET"})
    void retrieve_all_locations() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        VetEntity testuser = vetJpaRepository.save(new VetEntity("testuser", encoded));
        locationJpaRepository.save(new LocationEntity(testuser, "Petclinic", ZoneId.of("Europe/Vienna")));

        //act
        var result = mockMvc.perform(get("/locations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // controller returns 200 OK with PetResponse
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);

        assertThat(node.isArray()).isTrue();
        JsonNode locationNode = node.elements().next();

        assertThat(locationNode.has("id")).isTrue();
        assertThat(locationNode.get("name").asText()).isEqualTo("Petclinic");

        // also verify persisted
        var saved = locationJpaRepository.findByVetAndName(testuser, "Petclinic");
        assertThat(saved).isPresent();
    }


    @Test
    @WithMockUser(username = "testuser", roles = {"VET"})
    void post_creates_new_location() throws Exception {
        //arrange
        // seed existing user
        var encoded = passwordEncoder.encode("already");
        VetEntity testuser = vetJpaRepository.save(new VetEntity("testuser", encoded));
        assertThat(testuser.getId()).isNotNull();

        LocationResponse request = new LocationResponse(
                1L,
                "PetClinix",
                "Europe/Vienna",
                asList(new OpeningPeriodResponse(1, LocalTime.of(9,0), LocalTime.of(12,0), 1)),
                asList(new OpeningExceptionResponse(LocalDate.of(2025,1,1), true, "New Year's Day"))
        );
        var requestJson = objectMapper.writeValueAsString(request);

        //act
        var result = mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        String body = result.getResponse().getContentAsString();
        JsonNode petNode = objectMapper.readTree(body);

        assertThat(petNode.has("id")).isTrue();
        assertThat(petNode.get("name").asText()).isEqualTo("PetClinix");

        // also verify persisted
        var saved = locationJpaRepository.findByName("PetClinix");
        assertThat(saved).isPresent();

    }
}
