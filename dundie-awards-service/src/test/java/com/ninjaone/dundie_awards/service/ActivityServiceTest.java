package com.ninjaone.dundie_awards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.ninjaone.dundie_awards.dto.ActivityResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityService activityService;

    @Captor
    private ArgumentCaptor<Activity> activityCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void recordPersistsEventWithTimestamp() {
        LocalDateTime before = LocalDateTime.now();

        activityService.record("employee.created id=1");

        verify(activityRepository).save(activityCaptor.capture());
        Activity saved = activityCaptor.getValue();
        assertThat(saved.getEvent()).isEqualTo("employee.created id=1");
        assertThat(saved.getOccurredAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void getActivitiesRequestsNewestFirstWithIdTiebreak() {
        when(activityRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        activityService.getActivities(2, 15);

        verify(activityRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort())
                .containsExactly(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
    }

    @Test
    void getActivitiesMapsContentAndPageMetadata() {
        LocalDateTime newer = LocalDateTime.of(2024, 1, 2, 10, 0);
        LocalDateTime older = LocalDateTime.of(2024, 1, 1, 10, 0);
        List<Activity> activities = List.of(
                activity(2L, newer, "employee.updated id=1"),
                activity(1L, older, "employee.created id=1"));
        when(activityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(activities, PageRequest.of(1, 2), 6));

        PageResponse<ActivityResponse> response = activityService.getActivities(1, 2);

        assertThat(response.content())
                .extracting(ActivityResponse::id, ActivityResponse::occurredAt, ActivityResponse::event)
                .containsExactly(
                        tuple(2L, newer, "employee.updated id=1"),
                        tuple(1L, older, "employee.created id=1"));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void getActivitiesReturnsEmptyPageWhenNoneExist() {
        when(activityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<ActivityResponse> response = activityService.getActivities(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    private static Activity activity(long id, LocalDateTime occurredAt, String event) {
        return Activity.builder().id(id).occurredAt(occurredAt).event(event).build();
    }
}
