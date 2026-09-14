package com.ninjaone.dundie_awards.dto;

import com.ninjaone.dundie_awards.model.Organization;

public record OrganizationResponse(long id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName());
    }
}
