package com.ninjaone.dundie_awards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import com.ninjaone.dundie_awards.repository.DundieAwardRepository;
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

/**
 * Deliberately does not import {@link SynchronousActivityConfiguration}: this is the one place the
 * real thread pool runs, so it covers the behaviour the rest of the suite makes deterministic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActivityAsyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DundieAwardRepository dundieAwardRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void resetData() {
        dundieAwardRepository.deleteAll();
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void activityIsRecordedOffTheRequestThread() throws Exception {
        mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationRequest("Dunder Mifflin"))))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(activityEvents()).hasSize(1));
        assertThat(activityEvents().get(0)).startsWith("organization.created id=");
    }

    @Test
    void rolledBackWriteNeverRecordsAnActivity() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(put("/organizations/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationRequest(" "))))
                .andExpect(status().isBadRequest());

        // stays empty for the whole window rather than merely being empty at first look
        await().during(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(activityEvents()).isEmpty());
    }

    private List<String> activityEvents() {
        return activityRepository.findAll().stream().map(Activity::getEvent).toList();
    }
}
