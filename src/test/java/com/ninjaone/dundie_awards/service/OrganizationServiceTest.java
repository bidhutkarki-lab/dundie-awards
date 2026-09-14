package com.ninjaone.dundie_awards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.dto.OrganizationResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.exception.OrganizationHasEmployeesException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class OrganizationServiceTest {

    private static final long ORGANIZATION_ID = 10L;
    private static final OrganizationRequest REQUEST = new OrganizationRequest("Dunder Mifflin");

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private OrganizationService organizationService;

    @Captor
    private ArgumentCaptor<Organization> organizationCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void getOrganizationsRequestsStableIdOrder() {
        when(organizationRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        organizationService.getOrganizations(2, 15);

        verify(organizationRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void getOrganizationsMapsContentAndPageMetadata() {
        when(organizationRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(organization(ORGANIZATION_ID, "Dunder Mifflin"), organization(20L, "Sabre")),
                PageRequest.of(1, 2),
                6));

        PageResponse<OrganizationResponse> response = organizationService.getOrganizations(1, 2);

        assertThat(response.content())
                .extracting(OrganizationResponse::id, OrganizationResponse::name)
                .containsExactly(tuple(ORGANIZATION_ID, "Dunder Mifflin"), tuple(20L, "Sabre"));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void getOrganizationsReturnsEmptyPageWhenNoneExist() {
        when(organizationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<OrganizationResponse> response = organizationService.getOrganizations(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void getOrganizationMapsOrganizationToResponse() {
        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(organization(ORGANIZATION_ID, "Dunder Mifflin")));

        assertThat(organizationService.getOrganization(ORGANIZATION_ID))
                .isEqualTo(new OrganizationResponse(ORGANIZATION_ID, "Dunder Mifflin"));
    }

    @Test
    void createOrganizationSavesOrganization() {
        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(organization(ORGANIZATION_ID, "Dunder Mifflin"));

        OrganizationResponse response = organizationService.createOrganization(REQUEST);

        verify(organizationRepository).save(organizationCaptor.capture());
        assertThat(organizationCaptor.getValue().getName()).isEqualTo("Dunder Mifflin");
        assertThat(response).isEqualTo(new OrganizationResponse(ORGANIZATION_ID, "Dunder Mifflin"));
        verify(activityService).record("organization.created id=" + ORGANIZATION_ID);
    }

    @Test
    void updateOrganizationAppliesRequestToExistingOrganization() {
        Organization existing = organization(ORGANIZATION_ID, "Dunder Mifflin");
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(organizationRepository.save(existing)).thenReturn(existing);

        OrganizationResponse response =
                organizationService.updateOrganization(ORGANIZATION_ID, new OrganizationRequest("Sabre"));

        assertThat(existing.getName()).isEqualTo("Sabre");
        assertThat(response.name()).isEqualTo("Sabre");
        verify(activityService).record("organization.updated id=" + ORGANIZATION_ID);
    }

    @Test
    void deleteOrganizationRemovesOrganizationWithoutEmployees() {
        Organization existing = organization(ORGANIZATION_ID, "Dunder Mifflin");
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByOrganizationId(ORGANIZATION_ID)).thenReturn(false);

        organizationService.deleteOrganization(ORGANIZATION_ID);

        verify(organizationRepository).delete(existing);
        verify(activityService).record("organization.deleted id=" + ORGANIZATION_ID);
    }

    @Test
    void deleteOrganizationThrowsWhenEmployeesStillReferenceIt() {
        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(organization(ORGANIZATION_ID, "Dunder Mifflin")));
        when(employeeRepository.existsByOrganizationId(ORGANIZATION_ID)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.deleteOrganization(ORGANIZATION_ID))
                .isInstanceOf(OrganizationHasEmployeesException.class)
                .hasMessage("Organization still has employees: " + ORGANIZATION_ID);
        verify(organizationRepository, never()).delete(any());
        verify(activityService, never()).record(anyString());
    }

    @ParameterizedTest(name = "organization missing: {0}")
    @MethodSource("organizationLookupOperations")
    void operationThrowsWhenOrganizationMissing(String operationName, Consumer<OrganizationService> operation) {
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operation.accept(organizationService))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessage("Organization not found: " + ORGANIZATION_ID);
        verify(organizationRepository, never()).save(any());
        verify(organizationRepository, never()).delete(any());
        verify(activityService, never()).record(anyString());
    }

    private static Stream<Arguments> organizationLookupOperations() {
        return Stream.of(
                Arguments.of("getOrganization", asOperation(service -> service.getOrganization(ORGANIZATION_ID))),
                Arguments.of(
                        "updateOrganization",
                        asOperation(service -> service.updateOrganization(ORGANIZATION_ID, REQUEST))),
                Arguments.of(
                        "deleteOrganization", asOperation(service -> service.deleteOrganization(ORGANIZATION_ID))));
    }

    private static Consumer<OrganizationService> asOperation(Consumer<OrganizationService> operation) {
        return operation;
    }

    private static Organization organization(long id, String name) {
        return Organization.builder().id(id).name(name).build();
    }
}
