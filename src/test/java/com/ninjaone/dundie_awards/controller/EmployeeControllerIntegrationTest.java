package com.ninjaone.dundie_awards.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ninjaone.dundie_awards.TestcontainersConfiguration;
import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
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
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization;

    @BeforeEach
    void resetData() {
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
        organization = organizationRepository.save(new Organization("Dunder Mifflin"));
    }

    @Test
    void getAllEmployeesReturnsStoredEmployees() throws Exception {
        employeeRepository.save(new Employee("Michael", "Scott", organization));
        employeeRepository.save(new Employee("Dwight", "Schrute", organization));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").value("Michael"))
                .andExpect(jsonPath("$[0].organizationName").value("Dunder Mifflin"))
                .andExpect(jsonPath("$[1].firstName").value("Dwight"));
    }

    @Test
    void getAllEmployeesReturnsEmptyArrayWhenNoneExist() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
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
    }

    @Test
    void deleteEmployeeRemovesEmployee() throws Exception {
        Employee saved = employeeRepository.save(new Employee("Michael", "Scott", organization));

        mockMvc.perform(delete("/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        assertThat(employeeRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteEmployeeReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/employees/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: 9999"));
    }
}
