package com.skillsync.backend.controller;

import com.skillsync.backend.entity.University;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.UniversityRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {
        "http://localhost:5173",
        "https://skillsync-frontend-iilh.onrender.com"
    }
)
public class AuthController {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;

    public AuthController(
            UserRepository userRepository,
            UniversityRepository universityRepository) {

        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
    }


    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequest request) {

        if (request.name() == null ||
                request.name().isBlank()) {

            return "Name is required";
        }

        if (request.email() == null ||
                request.email().isBlank()) {

            return "Email is required";
        }

        if (request.password() == null ||
                request.password().isBlank()) {

            return "Password is required";
        }

        if (request.role() == null ||
                request.role().isBlank()) {

            return "Role is required";
        }


        String email =
                request.email()
                        .trim()
                        .toLowerCase();


        if (userRepository.existsByEmail(email)) {
            return "Email already registered";
        }


        String emailDomain =
                getDomain(email);


        if (emailDomain == null) {
            return "Invalid email address";
        }


        University university =
                universityRepository
                        .findByEmailDomain(emailDomain)
                        .orElse(null);


        if (university == null) {
            return "University email domain is not registered";
        }


        User.Role selectedRole;

        try {

            selectedRole =
                    User.Role.valueOf(
                            request.role()
                                    .trim()
                                    .toUpperCase()
                    );

        } catch (Exception e) {

            return "Invalid role";
        }


        if (selectedRole == User.Role.ADMIN) {

            return "Administrator accounts cannot be created through registration";
        }


        User user = new User();

        user.setName(
                request.name().trim()
        );

        user.setEmail(email);

        user.setPassword(
                request.password()
        );

        user.setRole(selectedRole);

        user.setUniversity(university);


        userRepository.save(user);

        return "Registration successful";
    }


    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request) {


        if (request.email() == null ||
                request.email().isBlank()) {

            return new LoginResponse(
                    false,
                    "Email is required",
                    null,
                    null,
                    null,
                    null
            );
        }


        if (request.password() == null ||
                request.password().isBlank()) {

            return new LoginResponse(
                    false,
                    "Password is required",
                    null,
                    null,
                    null,
                    null
            );
        }


        if (request.role() == null ||
                request.role().isBlank()) {

            return new LoginResponse(
                    false,
                    "Account type is required",
                    null,
                    null,
                    null,
                    null
            );
        }


        String email =
                request.email()
                        .trim()
                        .toLowerCase();


        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);


        if (user == null) {

            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null,
                    null,
                    null
            );
        }


        if (!user.getPassword()
                .equals(request.password())) {

            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null,
                    null,
                    null
            );
        }


        if (!user.getRole()
                .name()
                .equals(
                        request.role()
                                .trim()
                                .toUpperCase()
                )) {

            return new LoginResponse(
                    false,
                    "Incorrect account type",
                    null,
                    null,
                    null,
                    null
            );
        }


        return new LoginResponse(
                true,
                "Login successful",
                user.getId(),
                user.getName(),
                user.getRole().name(),
                user.getUniversity().getName()
        );
    }


    private String getDomain(String email) {

        int position =
                email.lastIndexOf("@");


        if (position == -1 ||
                position == email.length() - 1) {

            return null;
        }


        return email
                .substring(position + 1)
                .toLowerCase();
    }


    public record RegisterRequest(
            String name,
            String email,
            String password,
            String role
    ) {
    }


    public record LoginRequest(
            String email,
            String password,
            String role
    ) {
    }


    public record LoginResponse(
            boolean success,
            String message,
            Long id,
            String name,
            String role,
            String university
    ) {
    }
}