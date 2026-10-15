package com.skillsync.backend.repository;

import com.skillsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByUniversityId(Long universityId);

    List<User> findByUniversityIdAndRole(
            Long universityId,
            User.Role role
    );
}