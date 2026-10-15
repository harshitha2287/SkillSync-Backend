package com.skillsync.backend.repository;

import com.skillsync.backend.entity.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyProfileRepository
        extends JpaRepository<FacultyProfile, Long> {

    Optional<FacultyProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}