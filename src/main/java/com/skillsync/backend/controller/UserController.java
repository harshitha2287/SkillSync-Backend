package com.skillsync.backend.controller;

import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;

    public UserController(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }


    // =====================================================
    // FACULTY DIRECTORY
    // =====================================================

    @GetMapping("/faculty")
    public ResponseEntity<List<User>> getFaculty() {

        List<User> faculty =
                userRepository.findByRole(
                        User.Role.FACULTY
                );

        return ResponseEntity.ok(faculty);
    }


    // =====================================================
    // STUDENT DIRECTORY
    // =====================================================

    @GetMapping("/students")
    public ResponseEntity<List<User>> getStudents() {

        List<User> students =
                userRepository.findByRole(
                        User.Role.STUDENT
                );

        return ResponseEntity.ok(students);
    }


    // =====================================================
    // ALL USERS FROM UNIVERSITY
    // =====================================================

    @GetMapping("/university/{universityId}")
    public ResponseEntity<List<User>>
    getUniversityUsers(
            @PathVariable Long universityId
    ) {

        return ResponseEntity.ok(
                userRepository.findByUniversityId(
                        universityId
                )
        );
    }


    // =====================================================
    // FACULTY FROM UNIVERSITY
    // =====================================================

    @GetMapping(
            "/university/{universityId}/faculty"
    )
    public ResponseEntity<List<User>>
    getUniversityFaculty(
            @PathVariable Long universityId
    ) {

        return ResponseEntity.ok(
                userRepository
                        .findByUniversityIdAndRole(
                                universityId,
                                User.Role.FACULTY
                        )
        );
    }


    // =====================================================
    // USER BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id
    ) {

        return userRepository
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }
}