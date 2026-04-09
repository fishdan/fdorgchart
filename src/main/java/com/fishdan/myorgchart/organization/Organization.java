package com.fishdan.myorgchart.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Domain is required")
    private String domain;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationOwnership ownershipType = OrganizationOwnership.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationChartVisibility chartVisibility = OrganizationChartVisibility.PUBLIC;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OrganizationOwnership getOwnershipType() {
        return ownershipType == null ? OrganizationOwnership.OPEN : ownershipType;
    }

    public void setOwnershipType(OrganizationOwnership ownershipType) {
        this.ownershipType = ownershipType;
    }

    public boolean isOfficial() {
        return getOwnershipType() == OrganizationOwnership.OFFICIAL;
    }

    public OrganizationChartVisibility getChartVisibility() {
        return chartVisibility == null ? OrganizationChartVisibility.PUBLIC : chartVisibility;
    }

    public void setChartVisibility(OrganizationChartVisibility chartVisibility) {
        this.chartVisibility = chartVisibility;
    }

    public boolean isPrivateChart() {
        return getChartVisibility() == OrganizationChartVisibility.PRIVATE;
    }
}
