package com.ninjaone.dundie_awards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ninjaone.dundie_awards.dto.DundieAwardRequest;
import com.ninjaone.dundie_awards.dto.DundieAwardResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DundieAwardServiceTest {

    private static final long AWARD_ID = 100L;
    private static final long RECIPIENT_ID = 1L;
    private static final long GIVER_ID = 2L;
    private static final long ORGANIZATION_ID = 10L;
    private static final DundieAwardRequest REQUEST = new DundieAwardRequest(RECIPIENT_ID, GIVER_ID);
    private static final LocalDateTime AWARDED_AT = LocalDateTime.of(2024, 3, 1, 12, 0);

    @Mock
    private DundieAwardRepository dundieAwardRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private DundieAwardService dundieAwardService;

    @Captor
    private ArgumentCaptor<DundieAward> awardCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Captor
    private ArgumentCaptor<ActivityRecorded> activityCaptor;

    @Test
    void getAwardsRequestsNewestFirstOrder() {
        when(dundieAwardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        dundieAwardService.getAwards(2, 15);

        verify(dundieAwardRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort())
                .containsExactly(Sort.Order.desc("awardedAt"), Sort.Order.desc("id"));
    }

    @Test
    void getAwardsMapsContentAndPageMetadata() {
        when(dundieAwardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(award()),
                PageRequest.of(1, 2),
                4));

        PageResponse<DundieAwardResponse> response = dundieAwardService.getAwards(1, 2);

        assertThat(response.content()).singleElement().satisfies(award -> {
            assertThat(award.recipientId()).isEqualTo(RECIPIENT_ID);
            assertThat(award.recipientName()).isEqualTo("Michael Scott");
            assertThat(award.giverId()).isEqualTo(GIVER_ID);
            assertThat(award.giverName()).isEqualTo("Dwight Schrute");
            assertThat(award.organizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(award.organizationName()).isEqualTo("Dunder Mifflin");
            assertThat(award.awardedAt()).isEqualTo(AWARDED_AT);
        });
        assertThat(response.totalElements()).isEqualTo(4);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void getAwardMapsAwardToResponse() {
        when(dundieAwardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award()));

        DundieAwardResponse response = dundieAwardService.getAward(AWARD_ID);

        assertThat(response.recipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(response.recipientName()).isEqualTo("Michael Scott");
        assertThat(response.giverId()).isEqualTo(GIVER_ID);
        assertThat(response.giverName()).isEqualTo("Dwight Schrute");
        assertThat(response.organizationName()).isEqualTo("Dunder Mifflin");
    }

    @Test
    void getAwardThrowsWhenAwardMissing() {
        when(dundieAwardRepository.findById(AWARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dundieAwardService.getAward(AWARD_ID))
                .isInstanceOf(DundieAwardNotFoundException.class);
    }

    @Test
    void giveAwardIncrementsRecipientCounterNotGiver() {
        Employee recipient = employee(RECIPIENT_ID, "Michael", "Scott");
        Employee giver = employee(GIVER_ID, "Dwight", "Schrute");
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByIdAndDeletedAtIsNull(GIVER_ID)).thenReturn(Optional.of(giver));
        when(dundieAwardRepository.save(any(DundieAward.class))).thenReturn(award());

        dundieAwardService.giveAward(new DundieAwardRequest(RECIPIENT_ID, GIVER_ID));

        verify(employeeRepository).incrementAwardCount(RECIPIENT_ID);
        verify(employeeRepository, never()).incrementAwardCount(GIVER_ID);
    }

    @Test
    void giveAwardLeavesCounterAloneWhenAwardIsRejected() {
        assertThatThrownBy(() -> dundieAwardService.giveAward(new DundieAwardRequest(RECIPIENT_ID, RECIPIENT_ID)))
                .isInstanceOf(SelfAwardException.class);

        verify(employeeRepository, never()).incrementAwardCount(any());
    }

    @Test
    void giveAwardRecordsAward() {
        Employee recipient = employee(RECIPIENT_ID, "Michael", "Scott");
        Employee giver = employee(GIVER_ID, "Dwight", "Schrute");
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByIdAndDeletedAtIsNull(GIVER_ID)).thenReturn(Optional.of(giver));
        when(dundieAwardRepository.save(any(DundieAward.class))).thenReturn(award());

        DundieAwardResponse response = dundieAwardService.giveAward(REQUEST);

        verify(dundieAwardRepository).save(awardCaptor.capture());
        DundieAward saved = awardCaptor.getValue();
        assertThat(saved.getRecipient()).isSameAs(recipient);
        assertThat(saved.getGiver()).isSameAs(giver);
        assertThat(saved.getOrganization()).isSameAs(recipient.getOrganization());
        assertThat(saved.getAwardedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(AWARD_ID);
        assertActivityPublished("dundie_award.given recipientId=" + RECIPIENT_ID + " giverId=" + GIVER_ID);
    }

    @Test
    void giveAwardThrowsWhenRecipientMissing() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dundieAwardService.giveAward(REQUEST))
                .isInstanceOf(InvalidEmployeeReferenceException.class)
                .hasMessage("Employee not found: " + RECIPIENT_ID);
        verify(dundieAwardRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void giveAwardThrowsWhenGiverMissing() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID))
                .thenReturn(Optional.of(employee(RECIPIENT_ID, "Michael", "Scott")));
        when(employeeRepository.findByIdAndDeletedAtIsNull(GIVER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dundieAwardService.giveAward(REQUEST))
                .isInstanceOf(InvalidEmployeeReferenceException.class)
                .hasMessage("Employee not found: " + GIVER_ID);
        verify(dundieAwardRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void giveAwardThrowsWhenEmployeesBelongToDifferentOrganizations() {
        Employee recipient = employee(RECIPIENT_ID, "Michael", "Scott");
        Employee giver = Employee.builder()
                .id(GIVER_ID)
                .firstName("Jo")
                .lastName("Bennett")
                .organization(Organization.builder().id(20L).name("Sabre").build())
                .build();
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByIdAndDeletedAtIsNull(GIVER_ID)).thenReturn(Optional.of(giver));

        assertThatThrownBy(() -> dundieAwardService.giveAward(REQUEST))
                .isInstanceOf(CrossOrganizationAwardException.class);
        verify(dundieAwardRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void giveAwardThrowsWhenAnEmployeeHasNoOrganization() {
        Employee recipient = Employee.builder().id(RECIPIENT_ID).firstName("Creed").lastName("Bratton").build();
        when(employeeRepository.findByIdAndDeletedAtIsNull(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByIdAndDeletedAtIsNull(GIVER_ID))
                .thenReturn(Optional.of(employee(GIVER_ID, "Dwight", "Schrute")));

        assertThatThrownBy(() -> dundieAwardService.giveAward(REQUEST))
                .isInstanceOf(CrossOrganizationAwardException.class);
        verify(dundieAwardRepository, never()).save(any());
    }

    @Test
    void giveAwardThrowsWhenGiverAwardsThemselves() {
        assertThatThrownBy(() -> dundieAwardService.giveAward(new DundieAwardRequest(GIVER_ID, GIVER_ID)))
                .isInstanceOf(SelfAwardException.class);
        verify(dundieAwardRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    private static DundieAward award() {
        Employee recipient = employee(RECIPIENT_ID, "Michael", "Scott");
        DundieAward award = new DundieAward(
                recipient,
                employee(GIVER_ID, "Dwight", "Schrute"),
                recipient.getOrganization(),
                AWARDED_AT);
        ReflectionTestUtils.setField(award, "id", AWARD_ID);
        return award;
    }

    private void assertActivityPublished(String event) {
        verify(events).publishEvent(activityCaptor.capture());
        assertThat(activityCaptor.getValue().event()).isEqualTo(event);
    }

    private static Organization organization() {
        return Organization.builder().id(ORGANIZATION_ID).name("Dunder Mifflin").build();
    }

    private static Employee employee(long id, String firstName, String lastName) {
        return Employee.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .organization(organization())
                .build();
    }
}
