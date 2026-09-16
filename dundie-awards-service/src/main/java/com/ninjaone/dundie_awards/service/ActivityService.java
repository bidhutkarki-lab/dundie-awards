package com.ninjaone.dundie_awards.service;

import com.ninjaone.dundie_awards.config.AsyncConfig;
import com.ninjaone.dundie_awards.dto.ActivityResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.event.ActivityRecorded;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityService {

    // id breaks ties so paging stays stable when timestamps collide
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

    private final ActivityRepository activityRepository;

    public PageResponse<ActivityResponse> getActivities(int page, int size) {
        log.debug("Fetching activities page={} size={}", page, size);
        PageResponse<ActivityResponse> activities = PageResponse.from(
                activityRepository.findAll(PageRequest.of(page, size, NEWEST_FIRST))
                        .map(ActivityResponse::from));
        log.debug("Fetched {} of {} activities", activities.content().size(), activities.totalElements());
        return activities;
    }

    // the feed is an audit trail, not part of the business write. AFTER_COMMIT runs only after successful commit.
    // if the transaction rolls back, this listener does not run
    @Async(AsyncConfig.ACTIVITY_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivityRecorded(ActivityRecorded event) {
        try {
            activityRepository.save(new Activity(event.occurredAt(), event.event()));
            log.debug("Recorded activity: {}", event.event());
        } catch (RuntimeException exception) {
            // The business transaction has already committed, so this failure
            // cannot roll it back. There is no retry; the activity may be lost.
            // Reliable processing requires an outbox written in the business
            // transaction, plus a worker that retries failed entries.
            log.error("Failed to record activity: {}", event.event(), exception);
        }
    }
}
