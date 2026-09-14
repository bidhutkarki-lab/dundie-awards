package com.ninjaone.dundie_awards.service;

import java.time.LocalDateTime;

import com.ninjaone.dundie_awards.dto.ActivityResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    public void record(String event) {
        activityRepository.save(new Activity(LocalDateTime.now(), event));
        log.debug("Recorded activity: {}", event);
    }
}
