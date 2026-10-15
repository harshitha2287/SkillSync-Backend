package com.skillsync.backend.repository;

import com.skillsync.backend.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityRepository
        extends JpaRepository<Opportunity, Long> {

    List<Opportunity>
    findByUniversityIdOrderByDeadlineAsc(Long universityId);


    List<Opportunity>
    findByCreatedByIdOrderByDeadlineAsc(Long facultyId);
}