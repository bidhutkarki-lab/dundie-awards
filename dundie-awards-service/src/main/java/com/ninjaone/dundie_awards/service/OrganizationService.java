package com.ninjaone.dundie_awards.service;

import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.dto.OrganizationResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.event.ActivityRecorded;
import com.ninjaone.dundie_awards.exception.OrganizationHasEmployeesException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import java.time.LocalDateTime;
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
public class OrganizationService {

    // paging over an unordered result set can repeat or skip rows, so the order is fixed here
    private static final Sort BY_ID = Sort.by(Sort.Order.asc("id"));

    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher events;

    public PageResponse<OrganizationResponse> getOrganizations(int page, int size, String search) {
        log.debug("Fetching organizations page={} size={} search='{}'", page, size, search);
        PageResponse<OrganizationResponse> organizations = PageResponse.from(
                organizationRepository
                        .findByNameContainingIgnoreCaseAndDeletedAtIsNull(search, PageRequest.of(page, size, BY_ID))
                        .map(OrganizationResponse::from));
        log.debug("Fetched {} of {} organizations", organizations.content().size(), organizations.totalElements());
        return organizations;
    }

    public OrganizationResponse getOrganization(Long id) {
        log.debug("Fetching organization id={}", id);
        return OrganizationResponse.from(findOrganization(id));
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        log.debug("Creating organization name={}", request.name());
        OrganizationResponse saved =
                OrganizationResponse.from(organizationRepository.save(new Organization(request.name())));
        events.publishEvent(ActivityRecorded.of("organization.created id=" + saved.id()));
        log.info("Created organization id={}", saved.id());
        return saved;
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization organization = findOrganization(id);
        organization.setName(request.name());
        OrganizationResponse saved = OrganizationResponse.from(organizationRepository.save(organization));
        events.publishEvent(ActivityRecorded.of("organization.updated id=" + id));
        log.info("Updated organization id={}", id);
        return saved;
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = findOrganization(id);
        if (employeeRepository.existsByOrganizationIdAndDeletedAtIsNull(id)) {
            log.warn("Refusing to delete organization id={} because employees still reference it", id);
            throw new OrganizationHasEmployeesException(id);
        }
        // soft deleted so award history and former employees keep a valid organization
        organization.setDeletedAt(LocalDateTime.now());
        organizationRepository.save(organization);
        events.publishEvent(ActivityRecorded.of("organization.deleted id=" + id));
        log.info("Soft deleted organization id={}", id);
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> {
                    log.warn("Organization not found id={}", id);
                    return new OrganizationNotFoundException(id);
                });
    }
}
