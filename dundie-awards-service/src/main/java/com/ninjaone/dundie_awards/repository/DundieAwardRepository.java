package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.model.DundieAward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DundieAwardRepository extends JpaRepository<DundieAward, Long> {

    // the associations are eager and every response needs their names, so fetch them in one join
    // instead of letting Hibernate issue a select per row
    @Override
    @EntityGraph(attributePaths = {"recipient", "giver", "organization"})
    Page<DundieAward> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"recipient", "giver", "organization"})
    Optional<DundieAward> findById(Long id);
}
