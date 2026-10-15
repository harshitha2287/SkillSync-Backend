package com.skillsync.backend.repository;

import com.skillsync.backend.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborationRequestRepository
        extends JpaRepository<CollaborationRequest, Long> {

    List<CollaborationRequest> findByReceiverIdOrderByCreatedAtDesc(
            Long receiverId
    );

    List<CollaborationRequest> findBySenderIdOrderByCreatedAtDesc(
            Long senderId
    );

    boolean existsByProjectIdAndSenderIdAndReceiverIdAndStatus(
            Long projectId,
            Long senderId,
            Long receiverId,
            CollaborationRequest.Status status
    );
}