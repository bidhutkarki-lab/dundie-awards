package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Page<Organization> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    Optional<Organization> findByIdAndDeletedAtIsNull(Long id);
}
