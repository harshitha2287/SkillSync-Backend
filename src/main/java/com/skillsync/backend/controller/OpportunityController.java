package com.skillsync.backend.controller;

import com.skillsync.backend.entity.Opportunity;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.OpportunityRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
@CrossOrigin(
    origins = {
        "http://localhost:5173",
        "https://skillsync-frontend-iilh.onrender.com"
    }
)
public class OpportunityController {

    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;


    public OpportunityController(
            OpportunityRepository opportunityRepository,
            UserRepository userRepository) {

        this.opportunityRepository =
                opportunityRepository;

        this.userRepository =
                userRepository;
    }


    // =========================================================
    // GET OPPORTUNITIES FOR A USER
    //
    // Students and faculty only see opportunities
    // belonging to their university.
    // =========================================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOpportunities(
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


        if (user.getUniversity() == null) {

            return ResponseEntity.ok(
                    List.of()
            );
        }


        Long universityId =
                user.getUniversity()
                        .getId();


        List<Opportunity> opportunities =
                opportunityRepository
                        .findByUniversityIdOrderByDeadlineAsc(
                                universityId
                        );


        return ResponseEntity.ok(
                opportunities
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }


    // =========================================================
    // GET SINGLE OPPORTUNITY
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getOpportunity(
            @PathVariable Long id) {

        Opportunity opportunity =
                opportunityRepository
                        .findById(id)
                        .orElse(null);


        if (opportunity == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        return ResponseEntity.ok(
                toResponse(opportunity)
        );
    }


    // =========================================================
    // GET FACULTY'S OWN OPPORTUNITIES
    // =========================================================

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<?> getFacultyOpportunities(
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


        List<Opportunity> opportunities =
                opportunityRepository
                        .findByCreatedByIdOrderByDeadlineAsc(
                                facultyId
                        );


        return ResponseEntity.ok(
                opportunities
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }


    // =========================================================
    // CREATE OPPORTUNITY
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createOpportunity(
            @RequestBody OpportunityRequest request) {


        if (request.userId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User ID is required");
        }


        User faculty =
                userRepository
                        .findById(request.userId())
                        .orElse(null);


        if (faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User not found");
        }


        if (faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Only faculty members can create opportunities"
                    );
        }


        if (faculty.getUniversity() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Faculty university information is missing"
                    );
        }


        if (request.title() == null ||
                request.title().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Opportunity title is required"
                    );
        }


        Opportunity opportunity =
                new Opportunity();


        opportunity.setTitle(
                request.title().trim()
        );


        opportunity.setOrganization(
                request.organization()
        );


        opportunity.setType(
                request.type()
        );


        opportunity.setDescription(
                request.description()
        );


        opportunity.setSkills(
                request.skills()
        );


        opportunity.setDeadline(
                request.deadline()
        );


        opportunity.setRegistrationLink(
                request.registrationLink()
        );


        opportunity.setStatus(
                request.status() == null ||
                        request.status().trim().isEmpty()
                        ? "OPEN"
                        : request.status()
                                .trim()
                                .toUpperCase()
        );


        opportunity.setCreatedBy(
                faculty
        );


        opportunity.setUniversity(
                faculty.getUniversity()
        );


        opportunity.setCreatedAt(
                LocalDateTime.now()
        );


        Opportunity saved =
                opportunityRepository.save(
                        opportunity
                );


        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // =========================================================
    // UPDATE OPPORTUNITY
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOpportunity(
            @PathVariable Long id,
            @RequestBody OpportunityRequest request) {


        Opportunity opportunity =
                opportunityRepository
                        .findById(id)
                        .orElse(null);


        if (opportunity == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        if (request.userId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("User ID is required");
        }


        if (opportunity.getCreatedBy() == null ||
                !opportunity.getCreatedBy()
                        .getId()
                        .equals(request.userId())) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Only the faculty member who created this opportunity can update it"
                    );
        }


        if (request.title() != null &&
                !request.title()
                        .trim()
                        .isEmpty()) {

            opportunity.setTitle(
                    request.title().trim()
            );
        }


        opportunity.setOrganization(
                request.organization()
        );


        opportunity.setType(
                request.type()
        );


        opportunity.setDescription(
                request.description()
        );


        opportunity.setSkills(
                request.skills()
        );


        opportunity.setDeadline(
                request.deadline()
        );


        opportunity.setRegistrationLink(
                request.registrationLink()
        );


        if (request.status() != null &&
                !request.status()
                        .trim()
                        .isEmpty()) {

            opportunity.setStatus(
                    request.status()
                            .trim()
                            .toUpperCase()
            );
        }


        Opportunity updated =
                opportunityRepository.save(
                        opportunity
                );


        return ResponseEntity.ok(
                toResponse(updated)
        );
    }


    // =========================================================
    // DELETE OPPORTUNITY
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOpportunity(
            @PathVariable Long id,
            @RequestParam Long userId) {


        Opportunity opportunity =
                opportunityRepository
                        .findById(id)
                        .orElse(null);


        if (opportunity == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        if (opportunity.getCreatedBy() == null ||
                !opportunity.getCreatedBy()
                        .getId()
                        .equals(userId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Only the faculty member who created this opportunity can delete it"
                    );
        }


        opportunityRepository.delete(
                opportunity
        );


        return ResponseEntity.ok(
                "Opportunity deleted successfully"
        );
    }


    // =========================================================
    // RESPONSE DTO
    // =========================================================

    private OpportunityResponse toResponse(
            Opportunity opportunity) {

        return new OpportunityResponse(

                opportunity.getId(),

                opportunity.getTitle(),

                opportunity.getOrganization(),

                opportunity.getType(),

                opportunity.getDescription(),

                opportunity.getSkills(),

                opportunity.getDeadline(),

                opportunity.getRegistrationLink(),

                opportunity.getStatus(),

                opportunity.getCreatedBy() != null
                        ? opportunity
                                .getCreatedBy()
                                .getId()
                        : null,

                opportunity.getCreatedBy() != null
                        ? opportunity
                                .getCreatedBy()
                                .getName()
                        : null,

                opportunity.getCreatedAt()
        );
    }


    // =========================================================
    // REQUEST DTO
    // =========================================================

    public record OpportunityRequest(

            Long userId,

            String title,

            String organization,

            String type,

            String description,

            String skills,

            LocalDate deadline,

            String registrationLink,

            String status

    ) {
    }


    // =========================================================
    // RESPONSE DTO
    // =========================================================

    public record OpportunityResponse(

            Long id,

            String title,

            String organization,

            String type,

            String description,

            String skills,

            LocalDate deadline,

            String registrationLink,

            String status,

            Long createdById,

            String createdByName,

            LocalDateTime createdAt

    ) {
    }
}