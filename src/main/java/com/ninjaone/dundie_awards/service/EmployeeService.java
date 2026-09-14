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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    public EmployeeResponse getEmployee(Long id) {
        return EmployeeResponse.from(findEmployee(id));
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Organization organization = findOrganization(request.organizationId());
        Employee employee = new Employee(request.firstName(), request.lastName(), organization);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Organization organization = findOrganization(request.organizationId());
        Employee employee = findEmployee(id);
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setOrganization(organization);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public void deleteEmployee(Long id) {
        employeeRepository.delete(findEmployee(id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
    }
}
