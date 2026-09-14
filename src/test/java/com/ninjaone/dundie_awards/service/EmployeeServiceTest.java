package com.ninjaone.dundie_awards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.EmployeeResponse;
import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final long EMPLOYEE_ID = 1L;
    private static final long ORGANIZATION_ID = 10L;
    private static final EmployeeRequest REQUEST = new EmployeeRequest("Michael", "Scott", ORGANIZATION_ID);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    @Test
    void getAllEmployeesReturnsMappedResponses() {
        when(employeeRepository.findAll()).thenReturn(List.of(
                employee(EMPLOYEE_ID, "Michael", "Scott"),
                employee(2L, "Dwight", "Schrute")));

        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        assertThat(responses)
                .extracting(EmployeeResponse::id, EmployeeResponse::firstName, EmployeeResponse::organizationName)
                .containsExactly(
                        tuple(EMPLOYEE_ID, "Michael", "Dunder Mifflin"),
                        tuple(2L, "Dwight", "Dunder Mifflin"));
    }

    @Test
    void getAllEmployeesReturnsEmptyListWhenNoneExist() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        assertThat(employeeService.getAllEmployees()).isEmpty();
    }

    @ParameterizedTest(name = "maps employee with {0}")
    @MethodSource("employeeMappingCases")
    void getEmployeeMapsEmployeeToResponse(String caseName, Employee stored, EmployeeResponse expected) {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(stored));

        assertThat(employeeService.getEmployee(EMPLOYEE_ID)).isEqualTo(expected);
    }

    @Test
    void createEmployeeSavesEmployeeAgainstOrganization() {
        Organization organization = organization();
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(employee(EMPLOYEE_ID, "Michael", "Scott"));

        EmployeeResponse response = employeeService.createEmployee(REQUEST);

        verify(employeeRepository).save(employeeCaptor.capture());
        Employee saved = employeeCaptor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Michael");
        assertThat(saved.getLastName()).isEqualTo("Scott");
        assertThat(saved.getOrganization()).isSameAs(organization);
        assertThat(response.id()).isEqualTo(EMPLOYEE_ID);
    }

    @Test
    void updateEmployeeAppliesRequestToExistingEmployee() {
        Organization newOrganization = new Organization("Sabre");
        newOrganization.setId(20L);
        Employee existing = employee(EMPLOYEE_ID, "Michael", "Scott");
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(newOrganization));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse response =
                employeeService.updateEmployee(EMPLOYEE_ID, new EmployeeRequest("Michael", "Scarn", 20L));

        assertThat(existing.getFirstName()).isEqualTo("Michael");
        assertThat(existing.getLastName()).isEqualTo("Scarn");
        assertThat(existing.getOrganization()).isSameAs(newOrganization);
        assertThat(response.lastName()).isEqualTo("Scarn");
        assertThat(response.organizationName()).isEqualTo("Sabre");
    }

    @Test
    void deleteEmployeeRemovesExistingEmployee() {
        Employee existing = employee(EMPLOYEE_ID, "Michael", "Scott");
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(EMPLOYEE_ID);

        verify(employeeRepository).delete(existing);
    }

    @ParameterizedTest(name = "employee missing: {0}")
    @MethodSource("employeeLookupOperations")
    void operationThrowsWhenEmployeeMissing(String operationName, Consumer<EmployeeService> operation) {
        // only updateEmployee resolves an organization, and it does so before looking up the employee
        lenient().when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization()));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(employeeService))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).delete(any());
    }

    @ParameterizedTest(name = "organization missing: {0}")
    @MethodSource("organizationDependentOperations")
    void operationThrowsWhenOrganizationMissing(String operationName, Consumer<EmployeeService> operation) {
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(employeeService))
                .isInstanceOf(OrganizationNotFoundException.class);
        verify(employeeRepository, never()).save(any());
    }

    private static Stream<Arguments> employeeMappingCases() {
        Employee decorated = employee(EMPLOYEE_ID, "Michael", "Scott");
        decorated.setDundieAwards(3);

        Employee unassigned = new Employee("Creed", "Bratton", null);
        unassigned.setId(EMPLOYEE_ID);

        return Stream.of(
                Arguments.of(
                        "organization and awards present",
                        decorated,
                        new EmployeeResponse(EMPLOYEE_ID, "Michael", "Scott", 3, ORGANIZATION_ID, "Dunder Mifflin")),
                Arguments.of(
                        "no awards yet",
                        employee(EMPLOYEE_ID, "Pam", "Beesly"),
                        new EmployeeResponse(EMPLOYEE_ID, "Pam", "Beesly", null, ORGANIZATION_ID, "Dunder Mifflin")),
                Arguments.of(
                        "no organization",
                        unassigned,
                        new EmployeeResponse(EMPLOYEE_ID, "Creed", "Bratton", null, null, null)));
    }

    private static Stream<Arguments> employeeLookupOperations() {
        return Stream.of(
                Arguments.of("getEmployee", asOperation(service -> service.getEmployee(EMPLOYEE_ID))),
                Arguments.of("updateEmployee", asOperation(service -> service.updateEmployee(EMPLOYEE_ID, REQUEST))),
                Arguments.of("deleteEmployee", asOperation(service -> service.deleteEmployee(EMPLOYEE_ID))));
    }

    private static Stream<Arguments> organizationDependentOperations() {
        return Stream.of(
                Arguments.of("createEmployee", asOperation(service -> service.createEmployee(REQUEST))),
                Arguments.of("updateEmployee", asOperation(service -> service.updateEmployee(EMPLOYEE_ID, REQUEST))));
    }

    private static Consumer<EmployeeService> asOperation(Consumer<EmployeeService> operation) {
        return operation;
    }

    private static Organization organization() {
        Organization organization = new Organization("Dunder Mifflin");
        organization.setId(ORGANIZATION_ID);
        return organization;
    }

    private static Employee employee(long id, String firstName, String lastName) {
        Employee employee = new Employee(firstName, lastName, organization());
        employee.setId(id);
        return employee;
    }
}
