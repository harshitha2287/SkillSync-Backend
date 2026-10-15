package com.skillsync.backend.controller;

import com.skillsync.backend.entity.FacultyProfile;
import com.skillsync.backend.entity.StudentProfile;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.FacultyProfileRepository;
import com.skillsync.backend.repository.StudentProfileRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(
    origins = {
        "http://localhost:5173",
        "https://skillsync-frontend-iilh.onrender.com"
    }
)
public class ProfileController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;

    public ProfileController(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository) {

        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
    }


    /* =====================================================
       STUDENT PROFILE
    ===================================================== */

    @GetMapping("/student/{userId}")
    public Object getStudentProfile(
            @PathVariable Long userId) {

        return studentProfileRepository
                .findByUserId(userId)
                .orElse(null);
    }


    @PostMapping("/student/{userId}")
    public String saveStudentProfile(
            @PathVariable Long userId,
            @RequestBody StudentProfileRequest request) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "User not found";
        }

        if (user.getRole() != User.Role.STUDENT) {
            return "Only students can create student profiles";
        }

        StudentProfile profile =
                studentProfileRepository
                        .findByUserId(userId)
                        .orElse(new StudentProfile());

        profile.setUser(user);
        profile.setDepartment(request.department());
        profile.setYear(request.year());
        profile.setSkills(request.skills());
        profile.setInterests(request.interests());
        profile.setBio(request.bio());
        profile.setAchievements(request.achievements());
        profile.setCertifications(request.certifications());

        studentProfileRepository.save(profile);

        return "Student profile saved successfully";
    }


    /* =====================================================
       FACULTY PROFILE
    ===================================================== */

    @GetMapping("/faculty/{userId}")
    public Object getFacultyProfile(
            @PathVariable Long userId) {

        return facultyProfileRepository
                .findByUserId(userId)
                .orElse(null);
    }


    @PostMapping("/faculty/{userId}")
    public String saveFacultyProfile(
            @PathVariable Long userId,
            @RequestBody FacultyProfileRequest request) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return "User not found";
        }

        if (user.getRole() != User.Role.FACULTY) {
            return "Only faculty can create faculty profiles";
        }

        FacultyProfile profile =
                facultyProfileRepository
                        .findByUserId(userId)
                        .orElse(new FacultyProfile());

        profile.setUser(user);
        profile.setDepartment(request.department());
        profile.setDesignation(request.designation());
        profile.setExpertise(request.expertise());
        profile.setBio(request.bio());

        facultyProfileRepository.save(profile);

        return "Faculty profile saved successfully";
    }


    /* =====================================================
       REQUEST RECORDS
    ===================================================== */

    public record StudentProfileRequest(
            String department,
            Integer year,
            String skills,
            String interests,
            String bio,
            String achievements,
            String certifications
    ) {
    }


    public record FacultyProfileRequest(
            String department,
            String designation,
            String expertise,
            String bio
    ) {
    }
}