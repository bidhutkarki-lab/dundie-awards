package com.ninjaone.dundie_awards.service;

import java.util.List;

import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.EmployeeResponse;
import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;

    public List<EmployeeResponse> getAllEmployees() {
        log.debug("Fetching all employees");
        List<EmployeeResponse> employees = employeeRepository.findAll().stream()
                .map(EmployeeResponse::from)
                .toList();
        log.debug("Fetched {} employees", employees.size());
        return employees;
    }

    public EmployeeResponse getEmployee(Long id) {
        log.debug("Fetching employee id={}", id);
        return EmployeeResponse.from(findEmployee(id));
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.debug("Creating employee for organizationId={}", request.organizationId());
        Organization organization = findOrganization(request.organizationId());
        Employee employee = new Employee(request.firstName(), request.lastName(), organization);
        EmployeeResponse saved = EmployeeResponse.from(employeeRepository.save(employee));
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
        EmployeeResponse saved = EmployeeResponse.from(employeeRepository.save(employee));
        log.info("Updated employee id={} organizationId={}", id, request.organizationId());
        return saved;
    }

    public void deleteEmployee(Long id) {
        employeeRepository.delete(findEmployee(id));
        log.info("Deleted employee id={}", id);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found id={}", id);
                    return new EmployeeNotFoundException(id);
                });
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Organization not found id={}", id);
                    return new OrganizationNotFoundException(id);
                });
    }
}
