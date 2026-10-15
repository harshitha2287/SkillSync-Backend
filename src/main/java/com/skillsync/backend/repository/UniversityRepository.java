package com.skillsync.backend.repository;

import com.skillsync.backend.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {

    Optional<University> findByEmailDomain(String emailDomain);
}