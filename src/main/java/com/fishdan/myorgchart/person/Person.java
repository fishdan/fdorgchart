package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.organization.Organization;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "person", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "domain"}))
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Domain is required")
    private String domain;

    @NotBlank(message = "Department is required")
    private String department;

    private String supervisorEmail; // Can be null if no supervisor

    @ManyToOne
    @JoinColumn(name = "organization_id", referencedColumnName = "id")
    @NotNull(message = "Organization is required")
    private Organization organization;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSupervisorEmail() { return supervisorEmail; }
    public void setSupervisorEmail(String supervisorEmail) { this.supervisorEmail = supervisorEmail; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
}

