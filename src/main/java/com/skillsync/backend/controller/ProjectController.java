package com.skillsync.backend.controller;

import com.skillsync.backend.entity.Project;
import com.skillsync.backend.entity.ProjectMember;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.ProjectMemberRepository;
import com.skillsync.backend.repository.ProjectRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository) {

        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }


    // =========================================================
    // GET ALL PROJECTS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getAllProjects() {

        List<Project> projects =
                projectRepository.findAll();

        return ResponseEntity.ok(
                projects.stream()
                        .map(this::toProjectResponse)
                        .toList()
        );
    }


    // =========================================================
    // GET SINGLE PROJECT
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(
            @PathVariable Long id) {

        Project project =
                projectRepository
                        .findById(id)
                        .orElse(null);

        if (project == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                toProjectResponse(project)
        );
    }


    // =========================================================
    // GET PROJECTS BELONGING TO A STUDENT
    //
    // Includes:
    //
    // 1. Projects created by the student
    // 2. Projects the student joined after acceptance
    //
    // GET /api/projects/student/{userId}
    // =========================================================

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentProjects(
            @PathVariable Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);

        if (user == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User not found");
        }


        if (user.getRole() != User.Role.STUDENT) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Only students can access student projects"
                    );
        }


        // -----------------------------------------------------
        // Projects CREATED by the student
        // -----------------------------------------------------

        List<Project> createdProjects =
                projectRepository
                        .findByCreatedById(userId);


        // -----------------------------------------------------
        // Projects JOINED by the student
        // -----------------------------------------------------

        List<ProjectMember> memberships =
                projectMemberRepository
                        .findByStudentId(userId);


        // -----------------------------------------------------
        // Combine without duplicates
        // -----------------------------------------------------

        Map<Long, Project> uniqueProjects =
                new LinkedHashMap<>();


        // Created projects

        if (createdProjects != null) {

            for (Project project : createdProjects) {

                if (project == null) {
                    continue;
                }

                if (project.getId() == null) {
                    continue;
                }

                uniqueProjects.put(
                        project.getId(),
                        project
                );
            }
        }


        // Joined projects

        if (memberships != null) {

            for (ProjectMember membership : memberships) {

                if (membership == null) {
                    continue;
                }

                Project project =
                        membership.getProject();

                if (project == null) {
                    continue;
                }

                if (project.getId() == null) {
                    continue;
                }

                uniqueProjects.put(
                        project.getId(),
                        project
                );
            }
        }


        // -----------------------------------------------------
        // IMPORTANT
        //
        // Do NOT return Project entities directly.
        // Return DTO objects instead.
        // -----------------------------------------------------

        List<ProjectResponse> response =
                uniqueProjects
                        .values()
                        .stream()
                        .map(this::toProjectResponse)
                        .toList();


        return ResponseEntity.ok(response);
    }


    // =========================================================
    // CREATE PROJECT
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createProject(
            @RequestBody ProjectRequest request) {

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Request body is required");
        }


        if (request.userId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User ID is required");
        }


        User user =
                userRepository
                        .findById(request.userId())
                        .orElse(null);


        if (user == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User not found");
        }


        if (user.getRole() != User.Role.STUDENT) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Only students can create projects"
                    );
        }


        if (request.title() == null ||
                request.title()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Project title is required"
                    );
        }


        if (user.getUniversity() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student university information is missing"
                    );
        }


        // -----------------------------------------------------
        // Create
        // -----------------------------------------------------

        Project project =
                new Project();


        project.setTitle(
                request.title().trim()
        );


        project.setDescription(
                request.description()
        );


        project.setRequiredSkills(
                request.requiredSkills()
        );


        project.setTeamSize(
                request.teamSize()
        );


        // -----------------------------------------------------
        // Status
        // -----------------------------------------------------

        String requestedStatus =
                request.status();


        if (requestedStatus == null ||
                requestedStatus
                        .trim()
                        .isEmpty()) {

            project.setStatus(
                    Project.Status.PLANNING
            );

        } else {

            try {

                project.setStatus(
                        Project.Status.valueOf(
                                requestedStatus
                                        .trim()
                                        .toUpperCase()
                        )
                );

            } catch (IllegalArgumentException e) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid project status: "
                                        + requestedStatus
                        );
            }
        }


        // -----------------------------------------------------
        // Owner
        // -----------------------------------------------------

        project.setCreatedBy(user);


        // -----------------------------------------------------
        // University
        // -----------------------------------------------------

        project.setUniversity(
                user.getUniversity()
        );


        // -----------------------------------------------------
        // Save
        // -----------------------------------------------------

        Project saved =
                projectRepository.save(project);


        // -----------------------------------------------------
        // Return safe DTO
        // -----------------------------------------------------

        return ResponseEntity.ok(
                toProjectResponse(saved)
        );
    }


    // =========================================================
    // UPDATE PROJECT
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectRequest request) {

        Project project =
                projectRepository
                        .findById(id)
                        .orElse(null);


        if (project == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Request body is required");
        }


        // -----------------------------------------------------
        // Only owner can update
        // -----------------------------------------------------

        if (request.userId() != null &&
                project.getCreatedBy() != null &&
                !project.getCreatedBy()
                        .getId()
                        .equals(request.userId())) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Only the project owner can update this project"
                    );
        }


        // -----------------------------------------------------
        // Title
        // -----------------------------------------------------

        if (request.title() != null &&
                !request.title()
                        .trim()
                        .isEmpty()) {

            project.setTitle(
                    request.title().trim()
            );
        }


        // -----------------------------------------------------
        // Description
        // -----------------------------------------------------

        project.setDescription(
                request.description()
        );


        // -----------------------------------------------------
        // Required skills
        // -----------------------------------------------------

        project.setRequiredSkills(
                request.requiredSkills()
        );


        // -----------------------------------------------------
        // Team size
        // -----------------------------------------------------

        if (request.teamSize() != null) {

            project.setTeamSize(
                    request.teamSize()
            );
        }


        // -----------------------------------------------------
        // Status
        // -----------------------------------------------------

        if (request.status() != null &&
                !request.status()
                        .trim()
                        .isEmpty()) {

            try {

                project.setStatus(
                        Project.Status.valueOf(
                                request.status()
                                        .trim()
                                        .toUpperCase()
                        )
                );

            } catch (IllegalArgumentException e) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid project status: "
                                        + request.status()
                        );
            }
        }


        // -----------------------------------------------------
        // Save
        // -----------------------------------------------------

        Project updated =
                projectRepository.save(project);


        return ResponseEntity.ok(
                toProjectResponse(updated)
        );
    }


    // =========================================================
    // DELETE PROJECT
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @PathVariable Long id) {

        Project project =
                projectRepository
                        .findById(id)
                        .orElse(null);


        if (project == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        projectRepository.delete(project);


        return ResponseEntity.ok(
                "Project deleted successfully"
        );
    }


    // =========================================================
    // FACULTY PROJECTS
    //
    // Faculty can see projects from their university.
    // =========================================================

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<?> getFacultyProjects(
            @PathVariable Long facultyId) {

        User faculty =
                userRepository
                        .findById(facultyId)
                        .orElse(null);


        if (faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Faculty not found");
        }


        if (faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "User is not a faculty member"
                    );
        }


        if (faculty.getUniversity() == null) {

            return ResponseEntity.ok(
                    new ArrayList<>()
            );
        }


        Long universityId =
                faculty.getUniversity()
                        .getId();


        List<Project> allProjects =
                projectRepository.findAll();


        List<ProjectResponse> universityProjects =
                allProjects
                        .stream()
                        .filter(project ->
                                project.getUniversity() != null
                                        &&
                                universityId.equals(
                                        project
                                                .getUniversity()
                                                .getId()
                                )
                        )
                        .map(this::toProjectResponse)
                        .toList();


        return ResponseEntity.ok(
                universityProjects
        );
    }


    // =========================================================
    // CONVERT PROJECT ENTITY -> SAFE RESPONSE
    //
    // This prevents Jackson from serializing:
    //
    // Project -> members -> project -> members -> ...
    //
    // =========================================================

    private ProjectResponse toProjectResponse(
            Project project) {

        if (project == null) {
            return null;
        }


        Long createdById = null;
        String createdByName = null;


        if (project.getCreatedBy() != null) {

            createdById =
                    project
                            .getCreatedBy()
                            .getId();

            createdByName =
                    project
                            .getCreatedBy()
                            .getName();
        }


        Long universityId = null;


        if (project.getUniversity() != null) {

            universityId =
                    project
                            .getUniversity()
                            .getId();
        }


        return new ProjectResponse(

                project.getId(),

                project.getTitle(),

                project.getDescription(),

                project.getRequiredSkills(),

                project.getTeamSize(),

                project.getStatus() != null
                        ? project.getStatus().name()
                        : null,

                createdById,

                createdByName,

                universityId
        );
    }


    // =========================================================
    // PROJECT RESPONSE DTO
    // =========================================================

    public record ProjectResponse(

            Long id,

            String title,

            String description,

            String requiredSkills,

            Integer teamSize,

            String status,

            Long createdById,

            String createdByName,

            Long universityId

    ) {
    }


    // =========================================================
    // REQUEST DTO
    // =========================================================

    public record ProjectRequest(

            Long userId,

            String title,

            String description,

            String requiredSkills,

            Integer teamSize,

            String status

    ) {
    }
}