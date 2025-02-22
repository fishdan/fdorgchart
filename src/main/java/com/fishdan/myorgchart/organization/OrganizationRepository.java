package com.fishdan.myorgchart.organization;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Organization findByEmail(String email);

    boolean existsByDomain(String domain);

    // Case-insensitive domain lookup
    @Query("SELECT o FROM Organization o WHERE LOWER(o.domain) = LOWER(:domain)")
    Organization findByDomainIgnoreCase(String domain);

    // Redirect findByDomain to the case-insensitive lookup
    default Organization findByDomain(String domain) {
        return findByDomainIgnoreCase(domain);
    }
}



