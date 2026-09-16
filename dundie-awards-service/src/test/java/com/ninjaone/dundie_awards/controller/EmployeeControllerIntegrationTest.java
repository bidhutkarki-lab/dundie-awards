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
import com.ninjaone.dundie_awards.dto.EmployeeRequest;
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
class EmployeeControllerIntegrationTest {

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

    private Organization organization;

    @BeforeEach
    void resetData() {
        dundieAwardRepository.deleteAll();
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
        organization = organizationRepository.save(new Organization("Dunder Mifflin"));
    }

    @Test
    void getEmployeesReturnsStoredEmployees() throws Exception {
        employeeRepository.save(new Employee("Michael", "Scott", organization));
        employeeRepository.save(new Employee("Dwight", "Schrute", organization));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].firstName").value("Michael"))
                .andExpect(jsonPath("$.content[0].organizationName").value("Dunder Mifflin"))
                .andExpect(jsonPath("$.content[1].firstName").value("Dwight"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getEmployeesReturnsEmptyPageWhenNoneExist() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getEmployeesPagesInStableIdOrder() throws Exception {
        for (int i = 1; i <= 5; i++) {
            employeeRepository.save(new Employee("First" + i, "Last" + i, organization));
        }

        mockMvc.perform(get("/employees").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].firstName").value("First1"))
                .andExpect(jsonPath("$.content[1].firstName").value("First2"))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/employees").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("First3"))
                .andExpect(jsonPath("$.content[1].firstName").value("First4"));

        mockMvc.perform(get("/employees").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("First5"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getEmployeesReturnsEmptyContentBeyondLastPage() throws Exception {
        employeeRepository.save(new Employee("Michael", "Scott", organization));

        mockMvc.perform(get("/employees").param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getEmployeesRejectsInvalidPagingParams() throws Exception {
        mockMvc.perform(get("/employees").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page: must be greater than or equal to 0"));

        mockMvc.perform(get("/employees").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be greater than or equal to 1"));

        mockMvc.perform(get("/employees").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: must be less than or equal to 100"));
    }

    @Test
    void getEmployeesFiltersBySearchTerm() throws Exception {
        employeeRepository.save(new Employee("Michael", "Scott", organization));
        employeeRepository.save(new Employee("Dwight", "Schrute", organization));
        employeeRepository.save(new Employee("Pam", "Beesly", organization));

        mockMvc.perform(get("/employees").param("search", "sc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].lastName").value("Scott"))
                .andExpect(jsonPath("$.content[1].lastName").value("Schrute"));

        // matches across first and last name, case-insensitively
        mockMvc.perform(get("/employees").param("search", "PAM BEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Pam"));

        mockMvc.perform(get("/employees").param("search", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getEmployeesFiltersByOrganization() throws Exception {
        Organization other = organizationRepository.save(new Organization("Sabre"));
        employeeRepository.save(new Employee("Michael", "Scott", organization));
        employeeRepository.save(new Employee("Jo", "Bennett", other));

        mockMvc.perform(get("/employees").param("organizationId", String.valueOf(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Jo"));
    }

    @Test
    void getEmployeesSearchExcludesSoftDeleted() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));
        mockMvc.perform(delete("/employees/{id}", saved.getId())).andExpect(status().isOk());

        mockMvc.perform(get("/employees").param("search", "Scott"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void createEmployeePersistsEmployee() throws Exception {
        EmployeeRequest request = new EmployeeRequest("Michael", "Scott", organization.getId());

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Michael"))
                .andExpect(jsonPath("$.lastName").value("Scott"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId()));

        assertThat(employeeRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getFirstName()).isEqualTo("Michael");
                    assertThat(saved.getOrganization().getId()).isEqualTo(organization.getId());
                });
        long createdId = employeeRepository.findAll().get(0).getId();
        assertThat(activityEvents()).containsExactly("employee.created id=" + createdId);
    }

    @Test
    void createEmployeeRejectsBlankNames() throws Exception {
        EmployeeRequest request = new EmployeeRequest(" ", "", organization.getId());

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists());

        assertThat(employeeRepository.count()).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void createEmployeeRejectsMissingOrganizationId() throws Exception {
        EmployeeRequest request = new EmployeeRequest("Michael", "Scott", null);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.organizationId").exists());
    }

    @Test
    void createEmployeeReturnsBadRequestWhenOrganizationMissing() throws Exception {
        EmployeeRequest request = new EmployeeRequest("Michael", "Scott", 9999L);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization not found: 9999"));

        assertThat(employeeRepository.count()).isZero();
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void getEmployeeByIdReturnsEmployee() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Pam", "Beesly", organization));

        mockMvc.perform(get("/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.firstName").value("Pam"))
                .andExpect(jsonPath("$.organizationName").value("Dunder Mifflin"));
    }

    @Test
    void getEmployeeByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/employees/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));
    }

    @Test
    void updateEmployeeAppliesChanges() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));
        Organization newOrganization = organizationRepository.save(new Organization("Sabre"));
        EmployeeRequest request = new EmployeeRequest("Michael", "Scarn", newOrganization.getId());

        mockMvc.perform(put("/employees/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Scarn"))
                .andExpect(jsonPath("$.organizationName").value("Sabre"));

        Employee reloaded = employeeRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getLastName()).isEqualTo("Scarn");
        assertThat(reloaded.getOrganization().getId()).isEqualTo(newOrganization.getId());
        assertThat(activityEvents()).containsExactly("employee.updated id=" + saved.getId());
    }

    @Test
    void updateEmployeeReturnsNotFoundForUnknownId() throws Exception {
        EmployeeRequest request = new EmployeeRequest("Michael", "Scott", organization.getId());

        mockMvc.perform(put("/employees/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));
    }

    @Test
    void updateEmployeeReturnsBadRequestWhenOrganizationMissing() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));
        EmployeeRequest request = new EmployeeRequest("Michael", "Scarn", 9999L);

        mockMvc.perform(put("/employees/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization not found: 9999"));

        assertThat(employeeRepository.findById(saved.getId()).orElseThrow().getLastName()).isEqualTo("Scott");
        assertThat(activityEvents()).isEmpty();
    }

    @Test
    void deleteEmployeeSoftDeletesEmployee() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));

        mockMvc.perform(delete("/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        Employee reloaded = employeeRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(activityEvents()).containsExactly("employee.deleted id=" + saved.getId());
    }

    @Test
    void softDeletedEmployeeIsHiddenFromReads() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));
        mockMvc.perform(delete("/employees/{id}", saved.getId())).andExpect(status().isOk());

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/employees/{id}", saved.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/employees/{id}", saved.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/employees/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeRequest("Michael", "Scarn", organization.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEmployeeReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/employees/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));

        assertThat(activityEvents()).isEmpty();
    }

    private List<String> activityEvents() {
        return activityRepository.findAll().stream().map(Activity::getEvent).toList();
    }
}
