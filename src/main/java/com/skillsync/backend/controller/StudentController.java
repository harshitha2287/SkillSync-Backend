package com.skillsync.backend.controller;

import com.skillsync.backend.entity.FacultyProfile;
import com.skillsync.backend.entity.Project;
import com.skillsync.backend.entity.StudentProfile;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.FacultyProfileRepository;
import com.skillsync.backend.repository.ProjectRepository;
import com.skillsync.backend.repository.StudentProfileRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(
    origins = {
        "http://localhost:5173",
        "https://skillsync-frontend-iilh.onrender.com"
    }
)
public class StudentController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final ProjectRepository projectRepository;

    public StudentController(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            ProjectRepository projectRepository) {

        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.projectRepository = projectRepository;
    }


    /* =========================================================
       ALL STUDENTS
    ========================================================= */

    @GetMapping
    public List<Map<String, Object>> getStudents() {

        List<User> users = userRepository.findAll();

        List<Map<String, Object>> students =
                new ArrayList<>();

        for (User user : users) {

            if (user.getRole() != User.Role.STUDENT) {
                continue;
            }

            students.add(
                    createStudentResponse(user)
            );
        }

        return students;
    }


    /* =========================================================
       SINGLE STUDENT
    ========================================================= */

    @GetMapping("/{id}")
    public Object getStudent(
            @PathVariable Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElse(null);

        if (user == null) {
            return "Student not found";
        }

        if (user.getRole() != User.Role.STUDENT) {
            return "User is not a student";
        }

        return createStudentResponse(user);
    }


    /* =========================================================
       FACULTY DIRECTORY
       
       Example:
       GET /api/students/faculty/2

       2 = logged-in student's user ID
    ========================================================= */

    @GetMapping("/faculty/{studentUserId}")
    public List<Map<String, Object>> getFaculty(
            @PathVariable Long studentUserId) {

        User student =
                userRepository
                        .findById(studentUserId)
                        .orElse(null);

        if (student == null) {
            return new ArrayList<>();
        }

        if (student.getRole() != User.Role.STUDENT) {
            return new ArrayList<>();
        }

        Long universityId =
                student.getUniversity().getId();

        List<User> users =
                userRepository.findByUniversityIdAndRole(
                        universityId,
                        User.Role.FACULTY
                );

        List<Map<String, Object>> faculty =
                new ArrayList<>();

        for (User user : users) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "id",
                    user.getId()
            );

            data.put(
                    "userId",
                    user.getId()
            );

            data.put(
                    "name",
                    user.getName()
            );

            data.put(
                    "email",
                    user.getEmail()
            );

            data.put(
                    "role",
                    "FACULTY"
            );


            FacultyProfile profile =
                    facultyProfileRepository
                            .findByUserId(user.getId())
                            .orElse(null);


            if (profile != null) {

                data.put(
                        "department",
                        profile.getDepartment()
                );

                data.put(
                        "designation",
                        profile.getDesignation()
                );

                data.put(
                        "expertise",
                        profile.getExpertise()
                );

                data.put(
                        "bio",
                        profile.getBio()
                );

            } else {

                data.put(
                        "department",
                        ""
                );

                data.put(
                        "designation",
                        ""
                );

                data.put(
                        "expertise",
                        ""
                );

                data.put(
                        "bio",
                        ""
                );
            }

            faculty.add(data);
        }

        return faculty;
    }


    /* =========================================================
       CREATE STUDENT
       
       Registration now creates User accounts, so this endpoint
       is kept only for compatibility.
    ========================================================= */

    @PostMapping
    public String createStudent() {

        return "Student accounts are created through registration.";
    }


    /* =========================================================
       DELETE
    ========================================================= */

    @DeleteMapping("/{id}")
    public String deleteStudent(
            @PathVariable Long id) {

        return "Student accounts are managed through users.";
    }


    /* =========================================================
       STUDENT RESPONSE
    ========================================================= */

    private Map<String, Object> createStudentResponse(
            User user) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                user.getId()
        );

        data.put(
                "userId",
                user.getId()
        );

        data.put(
                "name",
                user.getName()
        );

        data.put(
                "email",
                user.getEmail()
        );

        data.put(
                "role",
                "STUDENT"
        );


        /* -----------------------------------------------------
           PROFILE
        ----------------------------------------------------- */

        StudentProfile profile =
                studentProfileRepository
                        .findByUserId(user.getId())
                        .orElse(null);


        if (profile != null) {

            data.put(
                    "department",
                    profile.getDepartment()
            );

            data.put(
                    "year",
                    profile.getYear()
            );

            data.put(
                    "skills",
                    profile.getSkills()
            );

            data.put(
                    "interests",
                    profile.getInterests()
            );

            data.put(
                    "bio",
                    profile.getBio()
            );

            data.put(
                    "achievements",
                    profile.getAchievements()
            );

            data.put(
                    "certifications",
                    profile.getCertifications()
            );

        } else {

            data.put(
                    "department",
                    ""
            );

            data.put(
                    "year",
                    null
            );

            data.put(
                    "skills",
                    ""
            );

            data.put(
                    "interests",
                    ""
            );

            data.put(
                    "bio",
                    ""
            );

            data.put(
                    "achievements",
                    ""
            );

            data.put(
                    "certifications",
                    ""
            );
        }


        /* -----------------------------------------------------
           PROJECTS CREATED BY STUDENT
        ----------------------------------------------------- */

        List<Project> projects =
                projectRepository
                        .findByCreatedById(
                                user.getId()
                        );


        List<Map<String, Object>> projectList =
                new ArrayList<>();


        for (Project project : projects) {

            Map<String, Object> projectData =
                    new LinkedHashMap<>();

            projectData.put(
                    "id",
                    project.getId()
            );

            projectData.put(
                    "title",
                    project.getTitle()
            );

            projectData.put(
                    "description",
                    project.getDescription()
            );

            projectData.put(
                    "requiredSkills",
                    project.getRequiredSkills()
            );

            projectData.put(
                    "status",
                    project.getStatus()
            );

            projectData.put(
                    "teamSize",
                    project.getTeamSize()
            );

            projectList.add(projectData);
        }


        data.put(
                "projects",
                projectList
        );


        return data;
    }
}