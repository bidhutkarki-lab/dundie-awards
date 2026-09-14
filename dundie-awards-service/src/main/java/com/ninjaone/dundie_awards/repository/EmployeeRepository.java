package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    // an empty search or a null organization means "no filter on that field"
    @Query("""
            select employee from Employee employee
            where employee.deletedAt is null
              and (:search = ''
                   or lower(concat(employee.firstName, ' ', employee.lastName))
                      like lower(concat('%', :search, '%')))
              and (:organizationId is null or employee.organization.id = :organizationId)
            """)
    Page<Employee> search(
            @Param("search") String search,
            @Param("organizationId") Long organizationId,
            Pageable pageable);

    // only employees who still work here should block deleting their organization
    boolean existsByOrganizationIdAndDeletedAtIsNull(Long organizationId);
}
