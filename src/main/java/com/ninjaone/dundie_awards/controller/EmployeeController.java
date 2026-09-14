package com.ninjaone.dundie_awards.controller;

import java.util.List;
import java.util.Map;

import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.EmployeeResponse;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final ActivityRepository activityRepository;

    // get all employees
    @GetMapping("/employees")
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    // create employee rest api
    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return organizationRepository.findById(request.organizationId())
                .map(organization -> {
                    Employee employee = new Employee(request.firstName(), request.lastName(), organization);
                    return ResponseEntity.ok(EmployeeResponse.from(employeeRepository.save(employee)));
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    // get employee by id rest api
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(employee -> ResponseEntity.ok(EmployeeResponse.from(employee)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // update employee rest api
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId()).orElse(null);
        if (organization == null) {
            return ResponseEntity.badRequest().build();
        }
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFirstName(request.firstName());
                    employee.setLastName(request.lastName());
                    employee.setOrganization(organization);
                    return ResponseEntity.ok(EmployeeResponse.from(employeeRepository.save(employee)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // delete employee rest api
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employeeRepository.delete(employee);
                    return ResponseEntity.ok(Map.of("deleted", Boolean.TRUE));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
