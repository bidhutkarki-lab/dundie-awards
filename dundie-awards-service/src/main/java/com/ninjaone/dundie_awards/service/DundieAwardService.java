package com.ninjaone.dundie_awards.service;

import java.time.LocalDateTime;

import com.ninjaone.dundie_awards.dto.DundieAwardRequest;
import com.ninjaone.dundie_awards.dto.DundieAwardResponse;
import com.ninjaone.dundie_awards.dto.LeaderboardEntry;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.event.ActivityRecorded;
import com.ninjaone.dundie_awards.exception.CrossOrganizationAwardException;
import com.ninjaone.dundie_awards.exception.DundieAwardNotFoundException;
import com.ninjaone.dundie_awards.exception.InvalidEmployeeReferenceException;
import com.ninjaone.dundie_awards.exception.SelfAwardException;
import com.ninjaone.dundie_awards.model.DundieAward;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.DundieAwardRepository;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DundieAwardService {

    // id breaks ties so paging stays stable when timestamps collide
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("awardedAt"), Sort.Order.desc("id"));

    private final DundieAwardRepository dundieAwardRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher events;

    public PageResponse<DundieAwardResponse> getAwards(int page, int size) {
        log.debug("Fetching dundie awards page={} size={}", page, size);
        PageResponse<DundieAwardResponse> awards = PageResponse.from(
                dundieAwardRepository.findAll(PageRequest.of(page, size, NEWEST_FIRST))
                        .map(DundieAwardResponse::from));
        log.debug("Fetched {} of {} dundie awards", awards.content().size(), awards.totalElements());
        return awards;
    }

    public DundieAwardResponse getAward(Long id) {
        log.debug("Fetching dundie award id={}", id);
        return DundieAwardResponse.from(dundieAwardRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Dundie award not found id={}", id);
                    return new DundieAwardNotFoundException(id);
                }));
    }

    public PageResponse<LeaderboardEntry> getLeaderboard(int page, int size) {
        log.debug("Fetching award leaderboard page={} size={}", page, size);
        PageResponse<LeaderboardEntry> leaderboard =
                PageResponse.from(employeeRepository.findLeaderboard(PageRequest.of(page, size)));
        log.debug("Fetched {} of {} leaderboard entries",
                leaderboard.content().size(), leaderboard.totalElements());
        return leaderboard;
    }

    @Transactional
    public DundieAwardResponse giveAward(DundieAwardRequest request) {
        if (request.recipientId().equals(request.giverId())) {
            log.warn("Rejecting self award employeeId={}", request.giverId());
            throw new SelfAwardException(request.giverId());
        }
        Employee recipient = findEmployee(request.recipientId());
        Employee giver = findEmployee(request.giverId());
        Organization organization = sharedOrganization(recipient, giver);

        DundieAward award = dundieAwardRepository.save(
                new DundieAward(recipient, giver, organization, LocalDateTime.now()));
        // same transaction as the insert, so the counter can never disagree with the rows.
        // the in-memory recipient is now stale, which is fine: nothing below reads its count
        employeeRepository.incrementAwardCount(recipient.getId());

        events.publishEvent(ActivityRecorded.of("dundie_award.given recipientId=" + recipient.getId()
                + " giverId=" + giver.getId()));
        log.info("Gave dundie award id={} recipientId={} giverId={} organizationId={}",
                award.getId(), recipient.getId(), giver.getId(), organization.getId());
        return DundieAwardResponse.from(award);
    }

    private Organization sharedOrganization(Employee recipient, Employee giver) {
        Organization organization = recipient.getOrganization();
        Organization giverOrganization = giver.getOrganization();
        if (organization == null || giverOrganization == null
                || organization.getId() != giverOrganization.getId()) {
            log.warn("Rejecting cross-organization award recipientId={} giverId={}",
                    recipient.getId(), giver.getId());
            throw new CrossOrganizationAwardException(recipient.getId(), giver.getId());
        }
        return organization;
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found id={}", id);
                    return new InvalidEmployeeReferenceException(id);
                });
    }
}
