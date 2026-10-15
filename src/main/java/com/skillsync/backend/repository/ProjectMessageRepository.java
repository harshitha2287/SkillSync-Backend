package com.skillsync.backend.repository;

import com.skillsync.backend.entity.ProjectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMessageRepository
        extends JpaRepository<ProjectMessage, Long> {

    @Query("""
        SELECT m
        FROM ProjectMessage m
        WHERE m.project.id = :projectId
        AND (
            (m.sender.id = :userId AND m.receiver.id = :otherUserId)
            OR
            (m.sender.id = :otherUserId AND m.receiver.id = :userId)
        )
        ORDER BY m.sentAt ASC
    """)
    List<ProjectMessage> findConversationMessages(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId
    );


    /*
     * Finds all messages where the specified faculty member
     * has participated in a conversation.
     *
     * DISTINCT is important because the same student may
     * have sent multiple messages.
     */
    @Query("""
        SELECT DISTINCT m
        FROM ProjectMessage m
        WHERE
            (m.sender.id = :facultyId
             AND m.receiver.role = com.skillsync.backend.entity.User$Role.STUDENT)
            OR
            (m.receiver.id = :facultyId
             AND m.sender.role = com.skillsync.backend.entity.User$Role.STUDENT)
        ORDER BY m.sentAt DESC
    """)
    List<ProjectMessage> findFacultyConversations(
            @Param("facultyId") Long facultyId
    );
}