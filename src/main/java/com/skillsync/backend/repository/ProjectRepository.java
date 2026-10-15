package com.skillsync.backend.repository;

import com.skillsync.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    List<Project> findByCreatedById(Long userId);

}