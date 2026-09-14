package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    // soft-deleted employees still hold a foreign key to their organization
    boolean existsByOrganizationId(Long organizationId);
}
