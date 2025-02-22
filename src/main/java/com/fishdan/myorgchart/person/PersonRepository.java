package com.fishdan.myorgchart.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    boolean existsByEmailAndDomain(String email, String domain);

    @Query("SELECT p FROM Person p WHERE LOWER(p.domain) = LOWER(:domain)")
    List<Person> findByDomainIgnoreCase(@Param("domain") String domain);

    // Default method to delegate to findByDomainIgnoreCase
    default List<Person> findByDomain(String domain) {
        return findByDomainIgnoreCase(domain);
    }
}
