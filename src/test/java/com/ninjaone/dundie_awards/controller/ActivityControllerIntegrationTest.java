package com.ninjaone.dundie_awards.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import com.ninjaone.dundie_awards.TestcontainersConfiguration;
import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActivityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void resetData() {
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void getActivitiesReturnsEmptyPageWhenNoneExist() throws Exception {
        mockMvc.perform(get("/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getActivitiesReturnsStoredActivity() throws Exception {
        activityRepository.save(new Activity(LocalDateTime.of(2024, 1, 1, 10, 0), "employee.created id=1"));

        mockMvc.perform(get("/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].event").value("employee.created id=1"))
                .andExpect(jsonPath("$.content[0].occurredAt").value("2024-01-01T10:00:00"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getActivitiesDefaultsToFirstPageOfTen() throws Exception {
        saveActivities(25);

        mockMvc.perform(get("/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].event").value("event-25"));
    }

    @Test
    void getActivitiesReturnsNewestFirstAcrossPageBoundary() throws Exception {
        saveActivities(5);

        mockMvc.perform(get("/activities").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].event").value("event-5"))
                .andExpect(jsonPath("$.content[1].event").value("event-4"))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/activities").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].event").value("event-3"))
                .andExpect(jsonPath("$.content[1].event").value("event-2"))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/activities").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].event").value("event-1"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getActivitiesPagesStablyWhenTimestampsCollide() throws Exception {
        LocalDateTime sameInstant = LocalDateTime.of(2024, 1, 1, 10, 0);
        activityRepository.save(new Activity(sameInstant, "first"));
        activityRepository.save(new Activity(sameInstant, "second"));
        activityRepository.save(new Activity(sameInstant, "third"));

        mockMvc.perform(get("/activities").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].event").value("third"))
                .andExpect(jsonPath("$.content[1].event").value("second"));

        mockMvc.perform(get("/activities").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].event").value("first"));
    }

    @Test
    void getActivitiesReturnsEmptyContentBeyondLastPage() throws Exception {
        saveActivities(3);

        mockMvc.perform(get("/activities").param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getActivitiesRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/activities").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page: must be greater than or equal to 0"));
    }

    @Test
    void getActivitiesRejectsZeroSize() throws Exception {
        mockMvc.perform(get("/activities").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be greater than or equal to 1"));
    }

    @Test
    void getActivitiesRejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/activities").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be less than or equal to 100"));
    }

    @Test
    void getActivitiesAcceptsMaximumSize() throws Exception {
        saveActivities(3);

        mockMvc.perform(get("/activities").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void activitiesRecordedByWritesAreExposedNewestFirst() throws Exception {
        String organizationJson = mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationRequest("Dunder Mifflin"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long organizationId = objectMapper.readTree(organizationJson).get("id").asLong();

        String employeeJson = mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeRequest("Michael", "Scott", organizationId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long employeeId = objectMapper.readTree(employeeJson).get("id").asLong();

        mockMvc.perform(put("/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeRequest("Michael", "Scarn", organizationId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/employees/{id}", employeeId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].event").value("employee.deleted id=" + employeeId))
                .andExpect(jsonPath("$.content[1].event").value("employee.updated id=" + employeeId))
                .andExpect(jsonPath("$.content[2].event").value("employee.created id=" + employeeId))
                .andExpect(jsonPath("$.content[3].event").value("organization.created id=" + organizationId));
    }

    @Test
    void failedWriteIsNotExposedInActivities() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(put("/organizations/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationRequest(" "))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private void saveActivities(int count) {
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 10, 0);
        IntStream.rangeClosed(1, count)
                .forEach(i -> activityRepository.save(new Activity(base.plusMinutes(i), "event-" + i)));
    }
}
