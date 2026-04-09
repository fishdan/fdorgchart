package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationChartVisibility;
import com.fishdan.myorgchart.person.Person;

import java.util.List;
import java.util.Set;

public class ManagedOrganizationView {

    private final Organization organization;
    private final List<Person> pendingMembers;
    private final List<Person> approvedMembers;
    private final Set<String> adminEmails;

    public ManagedOrganizationView(
        Organization organization,
        List<Person> pendingMembers,
        List<Person> approvedMembers,
        Set<String> adminEmails
    ) {
        this.organization = organization;
        this.pendingMembers = pendingMembers;
        this.approvedMembers = approvedMembers;
        this.adminEmails = adminEmails;
    }

    public Organization getOrganization() {
        return organization;
    }

    public List<Person> getPendingMembers() {
        return pendingMembers;
    }

    public List<Person> getApprovedMembers() {
        return approvedMembers;
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.toLowerCase());
    }

    public boolean adminEmail(String email) {
        return isAdminEmail(email);
    }

    public OrganizationChartVisibility getChartVisibility() {
        return organization.getChartVisibility();
    }
}
