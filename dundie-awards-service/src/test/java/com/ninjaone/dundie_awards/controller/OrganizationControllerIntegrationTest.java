package com.ninjaone.dundie_awards.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ninjaone.dundie_awards.SynchronousActivityConfiguration;
import com.ninjaone.dundie_awards.TestcontainersConfiguration;
import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.model.Activity;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SynchronousActivityConfiguration.class})
class OrganizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DundieAwardRepository dundieAwardRepository;

    @BeforeEach
    void resetData() {
        dundieAwardRepository.deleteAll();
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void getOrganizationsFiltersBySearchTerm() throws Exception {
        organizationRepository.save(new Organization("Dunder Mifflin"));
        organizationRepository.save(new Organization("Sabre"));

        mockMvc.perform(get("/organizations").param("search", "mifflin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Dunder Mifflin"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/organizations").param("search", "nothing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOrganizationsReturnsStoredOrganizations() throws Exception {
        organizationRepository.save(new Organization("Dunder Mifflin"));
        organizationRepository.save(new Organization("Sabre"));

        mockMvc.perform(get("/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Dunder Mifflin"))
                .andExpect(jsonPath("$.content[1].name").value("Sabre"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getOrganizationsReturnsEmptyPageWhenNoneExist() throws Exception {
        mockMvc.perform(get("/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getOrganizationsPagesInStableIdOrder() throws Exception {
        for (int i = 1; i <= 5; i++) {
            organizationRepository.save(new Organization("Org" + i));
        }

        mockMvc.perform(get("/organizations").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Org1"))
                .andExpect(jsonPath("$.content[1].name").value("Org2"))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/organizations").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Org5"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getOrganizationsReturnsEmptyContentBeyondLastPage() throws Exception {
        organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(get("/organizations").param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getOrganizationsRejectsInvalidPagingParams() throws Exception {
        mockMvc.perform(get("/organizations").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page: must be greater than or equal to 0"));

        mockMvc.perform(get("/organizations").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be greater than or equal to 1"));

        mockMvc.perform(get("/organizations").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be less than or equal to 100"));
    }

    @Test
    void createOrganizationPersistsOrganization() throws Exception {
        OrganizationRequest request = new OrganizationRequest("Dunder Mifflin");

        mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Dunder Mifflin"));

        assertThat(organizationRepository.findAll())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getName()).isEqualTo("Dunder Mifflin"));
        long createdId = organizationRepository.findAll().get(0).getId();
        assertThat(activityEvents()).containsExactly("organization.created id=" + createdId);
    }

    @Test
    void createOrganizationRejectsBlankName() throws Exception {
        OrganizationRequest request = new OrganizationRequest(" ");

        mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());

        assertThat(organizationRepository.count()).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void getOrganizationByIdReturnsOrganization() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(get("/organizations/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Dunder Mifflin"));
    }

    @Test
    void getOrganizationByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/organizations/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found: 9999"));

        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void updateOrganizationAppliesChanges() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));
        OrganizationRequest request = new OrganizationRequest("Sabre");

        mockMvc.perform(put("/organizations/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Sabre"));

        assertThat(organizationRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("Sabre");
        assertThat(activityEvents()).containsExactly("organization.updated id=" + saved.getId());
    }

    @Test
    void updateOrganizationReturnsNotFoundForUnknownId() throws Exception {
        OrganizationRequest request = new OrganizationRequest("Sabre");

        mockMvc.perform(put("/organizations/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found: 9999"));

        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void updateOrganizationRejectsBlankName() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(put("/organizations/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());

        assertThat(organizationRepository.findById(saved.getId()).orElseThrow().getName())
                .isEqualTo("Dunder Mifflin");
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void deleteOrganizationSoftDeletesOrganization() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));

        mockMvc.perform(delete("/organizations/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        assertThat(organizationRepository.findById(saved.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(activityEvents()).containsExactly("organization.deleted id=" + saved.getId());
    }

    @Test
    void softDeletedOrganizationIsHiddenFromReads() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));
        mockMvc.perform(delete("/organizations/{id}", saved.getId())).andExpect(status().isOk());

        mockMvc.perform(get("/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/organizations/{id}", saved.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/organizations/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrganizationIsAllowedWhenOnlySoftDeletedEmployeesRemain() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));
        Employee employee = employeeRepository.save(new Employee("Michael", "Scott", saved));

        // an active employee blocks the delete
        mockMvc.perform(delete("/organizations/{id}", saved.getId()))
                .andExpect(status().isConflict());

        employee.setDeletedAt(java.time.LocalDateTime.now());
        employeeRepository.save(employee);

        // once they are soft deleted the organization can go, and their row keeps a valid reference
        mockMvc.perform(delete("/organizations/{id}", saved.getId()))
                .andExpect(status().isOk());
        assertThat(employeeRepository.findById(employee.getId()).orElseThrow().getOrganization().getId())
                .isEqualTo(saved.getId());
    }

    @Test
    void deleteOrganizationReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/organizations/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found: 9999"));

        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void deleteOrganizationReturnsConflictWhenEmployeesStillReferenceIt() throws Exception {
        Organization saved = organizationRepository.save(new Organization("Dunder Mifflin"));
        employeeRepository.save(new Employee("Michael", "Scott", saved));

        mockMvc.perform(delete("/organizations/{id}", saved.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Organization still has employees: " + saved.getId()));

        assertThat(organizationRepository.findById(saved.getId())).isPresent();
        assertThat(activityEvents()).isEmpty();
    }

    private List<String> activityEvents() {
        return activityRepository.findAll().stream().map(Activity::getEvent).toList();
    }
}
