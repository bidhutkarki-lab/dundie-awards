package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import com.ninjaone.dundie_awards.dto.LeaderboardEntry;
import com.ninjaone.dundie_awards.model.DundieAward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    long countByRecipientId(Long recipientId);

    // totals are per recipient, reported against the organization they belong to today
    @Query(value = """
            select new com.ninjaone.dundie_awards.dto.LeaderboardEntry(
                recipient.id,
                concat(recipient.firstName, ' ', recipient.lastName),
                organization.name,
                count(award))
            from DundieAward award
            join award.recipient recipient
            left join recipient.organization organization
            where recipient.deletedAt is null
            group by recipient.id, recipient.firstName, recipient.lastName, organization.name
            order by count(award) desc, recipient.id asc
            """,
            countQuery = """
            select count(distinct award.recipient.id)
            from DundieAward award
            where award.recipient.deletedAt is null
            """)
    Page<LeaderboardEntry> findLeaderboard(Pageable pageable);
}
