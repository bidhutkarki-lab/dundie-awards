package com.ninjaone.dundie_awards.controller;

import java.util.List;
import java.util.Map;

import com.ninjaone.dundie_awards.dto.EmployeeRequest;
import com.ninjaone.dundie_awards.dto.EmployeeResponse;
import com.ninjaone.dundie_awards.service.EmployeeService;
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

    private final EmployeeService employeeService;

    @GetMapping("/employees")
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.createEmployee(request));
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(Map.of("deleted", Boolean.TRUE));
    }
}
