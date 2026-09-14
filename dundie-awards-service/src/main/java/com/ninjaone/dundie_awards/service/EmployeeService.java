package com.ninjaone.dundie_awards.service;

import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.EmployeeResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.InvalidOrganizationReferenceException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    // paging over an unordered result set can repeat or skip rows, so the order is fixed here
    private static final Sort BY_ID = Sort.by(Sort.Order.asc("id"));

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final DundieAwardService dundieAwardService;
    private final ActivityService activityService;

    public PageResponse<EmployeeResponse> getEmployees(int page, int size, String search, Long organizationId) {
        log.debug("Fetching employees page={} size={} search='{}' organizationId={}",
                page, size, search, organizationId);
        PageResponse<EmployeeResponse> employees = PageResponse.from(
                employeeRepository.search(search, organizationId, PageRequest.of(page, size, BY_ID))
                        .map(this::toResponse));
        log.debug("Fetched {} of {} employees", employees.content().size(), employees.totalElements());
        return employees;
    }

    public EmployeeResponse getEmployee(Long id) {
        log.debug("Fetching employee id={}", id);
        return toResponse(findEmployee(id));
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.debug("Creating employee for organizationId={}", request.organizationId());
        Organization organization = findOrganization(request.organizationId());
        Employee employee = new Employee(request.firstName(), request.lastName(), organization);
        EmployeeResponse saved = toResponse(employeeRepository.save(employee));
        activityService.record("employee.created id=" + saved.id());
        log.info("Created employee id={} organizationId={}", saved.id(), request.organizationId());
        return saved;
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Organization organization = findOrganization(request.organizationId());
        Employee employee = findEmployee(id);
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setOrganization(organization);
        EmployeeResponse saved = toResponse(employeeRepository.save(employee));
        activityService.record("employee.updated id=" + id);
        log.info("Updated employee id={} organizationId={}", id, request.organizationId());
        return saved;
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployee(id);
        employee.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        activityService.record("employee.deleted id=" + id);
        log.info("Soft deleted employee id={}", id);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.from(employee, dundieAwardService.countAwards(employee.getId()));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found id={}", id);
                    return new EmployeeNotFoundException(id);
                });
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> {
                    log.warn("Organization not found id={}", id);
                    return new InvalidOrganizationReferenceException(id);
                });
    }
}
