package com.skillsync.backend.repository;

import com.skillsync.backend.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    /*
     * Get all members belonging to a project.
     */
    List<ProjectMember> findByProjectId(Long projectId);


    /*
     * Get all projects/memberships belonging to a student.
     *
     * This is used by:
     * /api/projects/student/{userId}
     */
    List<ProjectMember> findByStudentId(Long studentId);


    /*
     * Check whether a student is already a member
     * of a project.
     */
    boolean existsByProjectIdAndStudentId(
            Long projectId,
            Long studentId
    );


    /*
     * Count members in a project.
     */
    long countByProjectId(Long projectId);


    /*
     * Find one student's membership in a project.
     */
    ProjectMember findByProjectIdAndStudentId(
            Long projectId,
            Long studentId
    );
}