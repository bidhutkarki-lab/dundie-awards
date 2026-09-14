package com.ninjaone.dundie_awards.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.ninjaone.dundie_awards.TestcontainersConfiguration;
import com.ninjaone.dundie_awards.dto.DundieAwardRequest;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.DundieAward;
import com.ninjaone.dundie_awards.model.Employee;
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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DundieAwardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DundieAwardRepository dundieAwardRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private CacheManager cacheManager;

    private Organization organization;
    private Employee recipient;
    private Employee giver;

    @BeforeEach
    void resetData() {
        dundieAwardRepository.deleteAll();
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        organization = organizationRepository.save(new Organization("Dunder Mifflin"));
        recipient = employeeRepository.save(new Employee("Michael", "Scott", organization));
        giver = employeeRepository.save(new Employee("Dwight", "Schrute", organization));
    }

    @Test
    void giveAwardPersistsAwardAndAccumulatesRecipientTotal() throws Exception {
        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), giver.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId").value(recipient.getId()))
                .andExpect(jsonPath("$.recipientName").value("Michael Scott"))
                .andExpect(jsonPath("$.giverId").value(giver.getId()))
                .andExpect(jsonPath("$.giverName").value("Dwight Schrute"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId()))
                .andExpect(jsonPath("$.organizationName").value("Dunder Mifflin"))
                .andExpect(jsonPath("$.awardedAt").exists());

        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), giver.getId())))
                .andExpect(status().isOk());

        assertThat(dundieAwardRepository.count()).isEqualTo(2);
        mockMvc.perform(get("/employees/{id}", recipient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dundieAwards").value(2));
        assertThat(activityEvents()).containsOnly(
                "dundie_award.given recipientId=" + recipient.getId() + " giverId=" + giver.getId());
    }

    @Test
    void giveAwardIsRejectedAcrossOrganizations() throws Exception {
        Organization otherOrganization = organizationRepository.save(new Organization("Sabre"));
        Employee outsider = employeeRepository.save(new Employee("Jo", "Bennett", otherOrganization));

        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), outsider.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Dundie awards can only be given within the same organization: recipientId="
                                + recipient.getId() + " giverId=" + outsider.getId()));

        assertThat(dundieAwardRepository.count()).isZero();
        assertThat(employeeAwardCount(recipient.getId())).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void awardCountCacheIsInvalidatedWhenANewAwardIsGiven() throws Exception {
        mockMvc.perform(get("/employees/{id}", recipient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dundieAwards").value(0));

        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), giver.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/employees/{id}", recipient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dundieAwards").value(1));
    }

    @Test
    void giveAwardIsRejectedWhenGiverAwardsThemselves() throws Exception {
        mockMvc.perform(postAward(new DundieAwardRequest(giver.getId(), giver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Employees cannot give a dundie award to themselves: employeeId=" + giver.getId()));

        assertThat(dundieAwardRepository.count()).isZero();
        assertThat(employeeAwardCount(giver.getId())).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void giveAwardIsRejectedWhenRecipientIsSoftDeleted() throws Exception {
        recipient.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(recipient);

        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), giver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: " + recipient.getId()));

        assertThat(dundieAwardRepository.count()).isZero();
    }

    @Test
    void giveAwardReturnsBadRequestWhenRecipientMissing() throws Exception {
        mockMvc.perform(postAward(new DundieAwardRequest(9999L, giver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));

        assertThat(dundieAwardRepository.count()).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void giveAwardReturnsBadRequestWhenGiverMissing() throws Exception {
        mockMvc.perform(postAward(new DundieAwardRequest(recipient.getId(), 9999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));

        assertThat(dundieAwardRepository.count()).isZero();
        assertThat(employeeAwardCount(recipient.getId())).isZero();
    }

    @Test
    void giveAwardRejectsMissingIds() throws Exception {
        mockMvc.perform(postAward(new DundieAwardRequest(null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recipientId").exists())
                .andExpect(jsonPath("$.giverId").exists());

        assertThat(dundieAwardRepository.count()).isZero();
    }

    @Test
    void getAwardsReturnsNewestFirst() throws Exception {
        dundieAwardRepository.save(
                new DundieAward(recipient, giver, organization, LocalDateTime.of(2024, 1, 1, 9, 0)));
        dundieAwardRepository.save(
                new DundieAward(giver, recipient, organization, LocalDateTime.of(2024, 2, 1, 9, 0)));

        mockMvc.perform(get("/dundie-awards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].recipientId").value(giver.getId()))
                .andExpect(jsonPath("$.content[1].recipientId").value(recipient.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAwardsRejectsInvalidPagingParams() throws Exception {
        mockMvc.perform(get("/dundie-awards").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be less than or equal to 100"));
    }

    @Test
    void getAwardByIdReturnsAward() throws Exception {
        DundieAward saved = dundieAwardRepository.save(
                new DundieAward(recipient, giver, organization, LocalDateTime.of(2024, 1, 1, 9, 0)));

        mockMvc.perform(get("/dundie-awards/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.recipientId").value(recipient.getId()))
                .andExpect(jsonPath("$.recipientName").value("Michael Scott"))
                .andExpect(jsonPath("$.giverName").value("Dwight Schrute"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId()));
    }

    @Test
    void leaderboardRanksRecipientsByAwardCount() throws Exception {
        Employee third = employeeRepository.save(new Employee("Creed", "Bratton", organization));
        // recipient: 3 awards, giver: 1, third: none
        for (int i = 0; i < 3; i++) {
            dundieAwardRepository.save(
                    new DundieAward(recipient, giver, organization, LocalDateTime.now()));
        }
        dundieAwardRepository.save(new DundieAward(giver, third, organization, LocalDateTime.now()));

        mockMvc.perform(get("/dundie-awards/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].recipientId").value(recipient.getId()))
                .andExpect(jsonPath("$.content[0].recipientName").value("Michael Scott"))
                .andExpect(jsonPath("$.content[0].organizationName").value("Dunder Mifflin"))
                .andExpect(jsonPath("$.content[0].awardCount").value(3))
                .andExpect(jsonPath("$.content[1].recipientId").value(giver.getId()))
                .andExpect(jsonPath("$.content[1].awardCount").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void leaderboardExcludesSoftDeletedRecipients() throws Exception {
        dundieAwardRepository.save(
                new DundieAward(recipient, giver, organization, LocalDateTime.now()));
        recipient.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(recipient);

        mockMvc.perform(get("/dundie-awards/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void leaderboardIsEmptyWhenNoAwardsExist() throws Exception {
        mockMvc.perform(get("/dundie-awards/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAwardByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/dundie-awards/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dundie award not found: 9999"));
    }

    private MockHttpServletRequestBuilder postAward(DundieAwardRequest request) {
        return post("/dundie-awards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private long employeeAwardCount(long employeeId) {
        return dundieAwardRepository.countByRecipientId(employeeId);
    }

    private List<String> activityEvents() {
        return activityRepository.findAll().stream().map(Activity::getEvent).toList();
    }
}
