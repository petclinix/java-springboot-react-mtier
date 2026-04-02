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
import tech.petclinix.persistence.jpa.LocationJpaRepository.Specifications;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;
import tech.petclinix.logic.domain.Location;
import tech.petclinix.logic.domain.Location.OpeningPeriodResponse;
import tech.petclinix.logic.domain.Location.OpeningOverrideResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        locationJpaRepository.save(new LocationEntity(testuser, "PetclinicX", ZoneId.of("Europe/Vienna")));

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
        assertThat(locationNode.get("name").asText()).isEqualTo("PetclinicX");

        // also verify persisted
        var saved = locationJpaRepository.findOne(Specifications.byVet(testuser));
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

        Location request = new Location(
                1L,
                "PetClinix",
                "Europe/Vienna",
                asList(new OpeningPeriodResponse(1, LocalTime.of(9,0), LocalTime.of(12,0), 1)),
                asList(new OpeningOverrideResponse(LocalDate.of(2025,1,1), null, null, true, "New Year's Day"))
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
        var saved = locationJpaRepository.findOne(Specifications.byVet(testuser));
        assertThat(saved).isPresent();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VET"})
    void retrieve_single_location() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        VetEntity testuser = vetJpaRepository.save(new VetEntity("testuser", encoded));
        LocationEntity location = locationJpaRepository.save(new LocationEntity(testuser, "Petclinic", ZoneId.of("Europe/Vienna")));

        //act + assert
        mockMvc.perform(get("/locations/" + location.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(location.getId()))
                .andExpect(jsonPath("$.name").value("Petclinic"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VET"})
    void put_updates_location() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        VetEntity testuser = vetJpaRepository.save(new VetEntity("testuser", encoded));
        LocationEntity location = locationJpaRepository.save(new LocationEntity(testuser, "Old Name", ZoneId.of("Europe/Vienna")));

        Location request = new Location(
                location.getId(),
                "New Name",
                "Europe/Berlin",
                asList(new OpeningPeriodResponse(2, LocalTime.of(8, 0), LocalTime.of(16, 0), 0)),
                asList(new OpeningOverrideResponse(LocalDate.of(2025, 12, 25), null, null, true, "Christmas"))
        );
        var requestJson = objectMapper.writeValueAsString(request);

        //act
        var result = mockMvc.perform(put("/locations/" + location.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        //assert
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("name").asText()).isEqualTo("New Name");
        assertThat(body.get("zoneId").asText()).isEqualTo("Europe/Berlin");
        assertThat(body.get("weeklyPeriods").size()).isEqualTo(1);
        assertThat(body.get("overrides").size()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VET"})
    void retrieve_single_location_returns_404_for_other_vets_location() throws Exception {
        //arrange
        var encoded = passwordEncoder.encode("already");
        vetJpaRepository.save(new VetEntity("testuser", encoded));
        VetEntity othervet = vetJpaRepository.save(new VetEntity("othervet", encoded));
        LocationEntity otherLocation = locationJpaRepository.save(new LocationEntity(othervet, "OtherClinic", ZoneId.of("Europe/Vienna")));

        //act + assert
        mockMvc.perform(get("/locations/" + otherLocation.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
