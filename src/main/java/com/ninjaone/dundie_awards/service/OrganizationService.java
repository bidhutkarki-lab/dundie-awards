package com.ninjaone.dundie_awards.service;

import com.ninjaone.dundie_awards.dto.OrganizationRequest;
import com.ninjaone.dundie_awards.dto.OrganizationResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.exception.OrganizationHasEmployeesException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ActivityService activityService;

    public PageResponse<OrganizationResponse> getOrganizations(int page, int size) {
        log.debug("Fetching organizations page={} size={}", page, size);
        PageResponse<OrganizationResponse> organizations = PageResponse.from(
                organizationRepository.findAll(PageRequest.of(page, size, BY_ID))
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
        activityService.record("organization.created id=" + saved.id());
        log.info("Created organization id={}", saved.id());
        return saved;
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization organization = findOrganization(id);
        organization.setName(request.name());
        OrganizationResponse saved = OrganizationResponse.from(organizationRepository.save(organization));
        activityService.record("organization.updated id=" + id);
        log.info("Updated organization id={}", id);
        return saved;
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = findOrganization(id);
        if (employeeRepository.existsByOrganizationId(id)) {
            log.warn("Refusing to delete organization id={} because employees still reference it", id);
            throw new OrganizationHasEmployeesException(id);
        }
        organizationRepository.delete(organization);
        activityService.record("organization.deleted id=" + id);
        log.info("Deleted organization id={}", id);
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Organization not found id={}", id);
                    return new OrganizationNotFoundException(id);
                });
    }
}
