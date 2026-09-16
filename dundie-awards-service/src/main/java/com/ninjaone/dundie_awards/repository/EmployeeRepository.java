package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.dto.LeaderboardEntry;
import com.ninjaone.dundie_awards.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    // an empty search or a null organization means "no filter on that field"
    // organization is eager and every response needs its name, so fetch it in the same query
    @EntityGraph(attributePaths = "organization")
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

    // incremented in the awarding transaction. done as one atomic statement rather than a
    // read-modify-write through the entity, which would lose concurrent increments
    @Modifying
    @Query("update Employee employee set employee.awardCount = employee.awardCount + 1 where employee.id = :employeeId")
    void incrementAwardCount(@Param("employeeId") Long employeeId);

    // reads the stored counter instead of aggregating dundie_awards, and the partial index
    // on (award_count desc, id) means paging touches only the rows it returns
    @Query(value = """
            select new com.ninjaone.dundie_awards.dto.LeaderboardEntry(
                employee.id,
                concat(employee.firstName, ' ', employee.lastName),
                organization.name,
                employee.awardCount)
            from Employee employee
            left join employee.organization organization
            where employee.deletedAt is null and employee.awardCount > 0
            order by employee.awardCount desc, employee.id asc
            """,
            countQuery = """
            select count(employee)
            from Employee employee
            where employee.deletedAt is null and employee.awardCount > 0
            """)
    Page<LeaderboardEntry> findLeaderboard(Pageable pageable);
}
