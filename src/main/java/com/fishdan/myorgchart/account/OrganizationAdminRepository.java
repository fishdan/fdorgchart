package com.fishdan.myorgchart.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationAdminRepository extends JpaRepository<OrganizationAdmin, Long> {

    boolean existsByOrganizationIdAndAccountId(Long organizationId, Long accountId);

    boolean existsByOrganizationDomainIgnoreCaseAndAccountEmail(String domain, String email);

    List<OrganizationAdmin> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<OrganizationAdmin> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    long countByOrganizationId(Long organizationId);

    long deleteByOrganizationIdAndAccountId(Long organizationId, Long accountId);

    long deleteByOrganizationDomainIgnoreCase(String domain);

    long deleteByAccountEmail(String email);
}
