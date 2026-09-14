package com.ninjaone.dundie_awards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.InvalidOrganizationReferenceException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final long EMPLOYEE_ID = 1L;
    private static final long ORGANIZATION_ID = 10L;
    private static final EmployeeRequest REQUEST = new EmployeeRequest("Michael", "Scott", ORGANIZATION_ID);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private DundieAwardService dundieAwardService;

    @InjectMocks
    private EmployeeService employeeService;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void getEmployeesRequestsStableIdOrder() {
        when(employeeRepository.search(eq(""), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        employeeService.getEmployees(2, 15, "", null);

        verify(employeeRepository).search(eq(""), isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void getEmployeesMapsContentAndPageMetadata() {
        when(employeeRepository.search(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(employee(EMPLOYEE_ID, "Michael", "Scott"), employee(2L, "Dwight", "Schrute")),
                PageRequest.of(1, 2),
                6));

        PageResponse<EmployeeResponse> response = employeeService.getEmployees(1, 2, "", null);

        assertThat(response.content())
                .extracting(EmployeeResponse::id, EmployeeResponse::firstName, EmployeeResponse::organizationName)
                .containsExactly(
                        tuple(EMPLOYEE_ID, "Michael", "Dunder Mifflin"),
                        tuple(2L, "Dwight", "Dunder Mifflin"));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void getEmployeesReturnsEmptyPageWhenNoneExist() {
        when(employeeRepository.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<EmployeeResponse> response = employeeService.getEmployees(0, 20, "", null);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @ParameterizedTest(name = "maps employee with {0}")
    @MethodSource("employeeMappingCases")
    void getEmployeeMapsEmployeeToResponse(
            String caseName, Employee stored, long awardCount, EmployeeResponse expected) {
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(stored));
        when(dundieAwardService.countAwards(EMPLOYEE_ID)).thenReturn(awardCount);

        assertThat(employeeService.getEmployee(EMPLOYEE_ID)).isEqualTo(expected);
    }

    @Test
    void createEmployeeSavesEmployeeAgainstOrganization() {
        Organization organization = organization();
        when(organizationRepository.findByIdAndDeletedAtIsNull(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(employee(EMPLOYEE_ID, "Michael", "Scott"));

        EmployeeResponse response = employeeService.createEmployee(REQUEST);

        verify(employeeRepository).save(employeeCaptor.capture());
        Employee saved = employeeCaptor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Michael");
        assertThat(saved.getLastName()).isEqualTo("Scott");
        assertThat(saved.getOrganization()).isSameAs(organization);
        assertThat(response.id()).isEqualTo(EMPLOYEE_ID);
        verify(activityService).record("employee.created id=" + EMPLOYEE_ID);
    }

    @Test
    void updateEmployeeAppliesRequestToExistingEmployee() {
        Organization newOrganization = Organization.builder().id(20L).name("Sabre").build();
        Employee existing = employee(EMPLOYEE_ID, "Michael", "Scott");
        when(organizationRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(newOrganization));
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse response =
                employeeService.updateEmployee(EMPLOYEE_ID, new EmployeeRequest("Michael", "Scarn", 20L));

        assertThat(existing.getFirstName()).isEqualTo("Michael");
        assertThat(existing.getLastName()).isEqualTo("Scarn");
        assertThat(existing.getOrganization()).isSameAs(newOrganization);
        assertThat(response.lastName()).isEqualTo("Scarn");
        assertThat(response.organizationName()).isEqualTo("Sabre");
        verify(activityService).record("employee.updated id=" + EMPLOYEE_ID);
    }

    @Test
    void deleteEmployeeMarksExistingEmployeeAsDeleted() {
        Employee existing = employee(EMPLOYEE_ID, "Michael", "Scott");
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(EMPLOYEE_ID);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(employeeRepository).save(existing);
        verify(employeeRepository, never()).delete(any());
        verify(activityService).record("employee.deleted id=" + EMPLOYEE_ID);
    }

    @ParameterizedTest(name = "employee missing: {0}")
    @MethodSource("employeeLookupOperations")
    void operationThrowsWhenEmployeeMissing(String operationName, Consumer<EmployeeService> operation) {
        // only updateEmployee resolves an organization, and it does so before looking up the employee
        lenient().when(organizationRepository.findByIdAndDeletedAtIsNull(ORGANIZATION_ID)).thenReturn(Optional.of(organization()));
        when(employeeRepository.findByIdAndDeletedAtIsNull(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(employeeService))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).delete(any());
        verify(activityService, never()).record(anyString());
    }

    @ParameterizedTest(name = "organization missing: {0}")
    @MethodSource("organizationDependentOperations")
    void operationThrowsWhenOrganizationMissing(String operationName, Consumer<EmployeeService> operation) {
        when(organizationRepository.findByIdAndDeletedAtIsNull(ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(employeeService))
                .isInstanceOf(InvalidOrganizationReferenceException.class);
        verify(employeeRepository, never()).save(any());
        verify(activityService, never()).record(anyString());
    }

    private static Stream<Arguments> employeeMappingCases() {
        Employee unassigned = Employee.builder()
                .id(EMPLOYEE_ID)
                .firstName("Creed")
                .lastName("Bratton")
                .build();

        return Stream.of(
                Arguments.of(
                        "organization and awards present",
                        employee(EMPLOYEE_ID, "Michael", "Scott"),
                        3L,
                        EmployeeResponse.builder()
                                .id(EMPLOYEE_ID)
                                .firstName("Michael")
                                .lastName("Scott")
                                .dundieAwards(3)
                                .organizationId(ORGANIZATION_ID)
                                .organizationName("Dunder Mifflin")
                                .build()),
                Arguments.of(
                        "no awards yet",
                        employee(EMPLOYEE_ID, "Pam", "Beesly"),
                        0L,
                        EmployeeResponse.builder()
                                .id(EMPLOYEE_ID)
                                .firstName("Pam")
                                .lastName("Beesly")
                                .organizationId(ORGANIZATION_ID)
                                .organizationName("Dunder Mifflin")
                                .build()),
                Arguments.of(
                        "no organization",
                        unassigned,
                        0L,
                        EmployeeResponse.builder()
                                .id(EMPLOYEE_ID)
                                .firstName("Creed")
                                .lastName("Bratton")
                                .build()));
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
        return Organization.builder().id(ORGANIZATION_ID).name("Dunder Mifflin").build();
    }

    private static Employee employee(long id, String firstName, String lastName) {
        return Employee.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .organization(organization())
                .build();
    }
}
