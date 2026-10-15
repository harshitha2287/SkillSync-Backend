package com.skillsync.backend.controller;

import com.skillsync.backend.entity.University;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.UniversityRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.http.ResponseEntity;
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

    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request) {

        try {

            if (request.name() == null ||
                    request.name().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Name is required");
            }

            if (request.email() == null ||
                    request.email().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Email is required");
            }

            if (request.password() == null ||
                    request.password().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Password is required");
            }

            if (request.role() == null ||
                    request.role().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Role is required");
            }

            String email =
                    request.email()
                            .trim()
                            .toLowerCase();

            if (userRepository.existsByEmail(email)) {

                return ResponseEntity.badRequest()
                        .body("Email already registered");
            }

            String emailDomain = getDomain(email);

            if (emailDomain == null) {

                return ResponseEntity.badRequest()
                        .body("Invalid email address");
            }

            University university =
                    universityRepository
                            .findByEmailDomain(emailDomain)
                            .orElse(null);

            if (university == null) {

                return ResponseEntity.badRequest()
                        .body(
                                "University email domain is not registered"
                        );
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

                return ResponseEntity.badRequest()
                        .body("Invalid role");
            }

            if (selectedRole == User.Role.ADMIN) {

                return ResponseEntity.badRequest()
                        .body(
                                "Administrator accounts cannot be created through registration"
                        );
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

            return ResponseEntity.ok(
                    "Registration successful"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Registration failed: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        try {

            // -------------------------------------------------
            // Validate email
            // -------------------------------------------------

            if (request.email() == null ||
                    request.email().isBlank()) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Email is required",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // Validate password
            // -------------------------------------------------

            if (request.password() == null ||
                    request.password().isBlank()) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Password is required",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // Validate role
            // -------------------------------------------------

            if (request.role() == null ||
                    request.role().isBlank()) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Account type is required",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // Normalize email
            // -------------------------------------------------

            String email =
                    request.email()
                            .trim()
                            .toLowerCase();

            // -------------------------------------------------
            // Find user
            // -------------------------------------------------

            User user =
                    userRepository
                            .findByEmail(email)
                            .orElse(null);

            if (user == null) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Invalid email or password",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // Check password
            // -------------------------------------------------

            if (user.getPassword() == null ||
                    !user.getPassword()
                            .equals(request.password())) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Invalid email or password",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // Check role
            // -------------------------------------------------

            if (user.getRole() == null) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "User account has no role assigned",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            if (!user.getRole()
                    .name()
                    .equals(
                            request.role()
                                    .trim()
                                    .toUpperCase()
                    )) {

                return ResponseEntity.ok(
                        new LoginResponse(
                                false,
                                "Incorrect account type",
                                null,
                                null,
                                null,
                                null
                        )
                );
            }

            // -------------------------------------------------
            // University
            // -------------------------------------------------

            String universityName = null;

            if (user.getUniversity() != null) {

                universityName =
                        user.getUniversity().getName();

            } else {

                /*
                 * Don't allow a NullPointerException to
                 * crash the login endpoint.
                 */

                universityName =
                        "University not assigned";
            }

            // -------------------------------------------------
            // Successful login
            // -------------------------------------------------

            return ResponseEntity.ok(
                    new LoginResponse(
                            true,
                            "Login successful",
                            user.getId(),
                            user.getName(),
                            user.getRole().name(),
                            universityName
                    )
            );

        } catch (Exception e) {

            /*
             * IMPORTANT:
             * This prints the actual exception into the
             * Render logs instead of silently returning 500.
             */

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            new LoginResponse(
                                    false,
                                    "Login failed: "
                                            + e.getMessage(),
                                    null,
                                    null,
                                    null,
                                    null
                            )
                    );
        }
    }

    // =========================================================
    // GET EMAIL DOMAIN
    // =========================================================

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

    // =========================================================
    // DTOs
    // =========================================================

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