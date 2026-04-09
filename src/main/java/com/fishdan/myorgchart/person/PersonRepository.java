package com.fishdan.myorgchart.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    boolean existsByEmailAndDomain(String email, String domain);

    @Query("SELECT p FROM Person p WHERE LOWER(p.domain) = LOWER(:domain)")
    List<Person> findByDomainIgnoreCase(@Param("domain") String domain);

    @Query("SELECT p FROM Person p WHERE LOWER(p.domain) = LOWER(:domain) AND p.approvalStatus = :approvalStatus")
    List<Person> findByDomainAndApprovalStatusIgnoreCase(
        @Param("domain") String domain,
        @Param("approvalStatus") PersonApprovalStatus approvalStatus
    );

    // Default method to delegate to findByDomainIgnoreCase
    default List<Person> findByDomain(String domain) {
        return findByDomainIgnoreCase(domain);
    }

    @Query("SELECT p FROM Person p WHERE LOWER(p.email) = LOWER(:email)")
    List<Person> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT p FROM Person p WHERE LOWER(p.email) = LOWER(:email) AND LOWER(p.domain) = LOWER(:domain)")
    Optional<Person> findByEmailAndDomainIgnoreCase(@Param("email") String email, @Param("domain") String domain);

    List<Person> findByOrganizationIdAndApprovalStatusOrderByFullNameAsc(Long organizationId, PersonApprovalStatus approvalStatus);

    List<Person> findByOrganizationIdOrderByFullNameAsc(Long organizationId);

    @Transactional
    long deleteByDomainIgnoreCase(String domain);

    @Transactional
    long deleteByEmailIgnoreCase(String email);
}
