package com.skillsync.backend.controller;

import com.skillsync.backend.entity.CollaborationRequest;
import com.skillsync.backend.entity.Project;
import com.skillsync.backend.entity.ProjectMember;
import com.skillsync.backend.entity.ProjectMessage;
import com.skillsync.backend.entity.StudentProfile;
import com.skillsync.backend.entity.User;

import com.skillsync.backend.repository.CollaborationRequestRepository;
import com.skillsync.backend.repository.ProjectMemberRepository;
import com.skillsync.backend.repository.ProjectMessageRepository;
import com.skillsync.backend.repository.ProjectRepository;
import com.skillsync.backend.repository.StudentProfileRepository;
import com.skillsync.backend.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/collaboration")
@CrossOrigin(origins = "http://localhost:5173")
public class CollaborationController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CollaborationRequestRepository collaborationRequestRepository;
    private final ProjectMessageRepository projectMessageRepository;

    public CollaborationController(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            ProjectMemberRepository projectMemberRepository,
            CollaborationRequestRepository collaborationRequestRepository,
            ProjectMessageRepository projectMessageRepository) {

        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.collaborationRequestRepository = collaborationRequestRepository;
        this.projectMessageRepository = projectMessageRepository;
    }

    // =========================================================
    // FIND TEAMMATES
    // =========================================================

    @GetMapping("/candidates/{projectId}")
    public Object getCandidates(@PathVariable Long projectId) {

        Project project = projectRepository
                .findById(projectId)
                .orElse(null);

        if (project == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Project not found");
        }

        if (project.getUniversity() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Project university is missing");
        }

        if (project.getCreatedBy() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Project owner is missing");
        }

        Long universityId = project.getUniversity().getId();
        Long ownerId = project.getCreatedBy().getId();

        List<User> students =
                userRepository.findByUniversityIdAndRole(
                        universityId,
                        User.Role.STUDENT
                );

        List<CandidateResponse> candidates = new ArrayList<>();

        for (User student : students) {

            if (student.getId().equals(ownerId)) {
                continue;
            }

            if (projectMemberRepository
                    .existsByProjectIdAndStudentId(
                            projectId,
                            student.getId())) {

                continue;
            }

            StudentProfile profile =
                    studentProfileRepository
                            .findByUserId(student.getId())
                            .orElse(null);

            int score = calculateMatchScore(
                    project.getRequiredSkills(),
                    profile
            );

            candidates.add(
                    new CandidateResponse(
                            student.getId(),
                            student.getName(),
                            student.getEmail(),

                            profile != null
                                    ? profile.getDepartment()
                                    : "",

                            profile != null
                                    ? profile.getYear()
                                    : null,

                            profile != null
                                    ? profile.getSkills()
                                    : "",

                            profile != null
                                    ? profile.getInterests()
                                    : "",

                            profile != null
                                    ? profile.getBio()
                                    : "",

                            profile != null
                                    ? profile.getAchievements()
                                    : "",

                            profile != null
                                    ? profile.getCertifications()
                                    : "",

                            score
                    )
            );
        }

        candidates.sort(
                (a, b) ->
                        Integer.compare(
                                b.matchScore(),
                                a.matchScore()
                        )
        );

        return ResponseEntity.ok(candidates);
    }

    // =========================================================
    // MATCH SCORE
    // =========================================================

    private int calculateMatchScore(
            String requiredSkills,
            StudentProfile profile) {

        if (requiredSkills == null ||
                requiredSkills.trim().isEmpty() ||
                profile == null) {

            return 0;
        }

        String skills =
                profile.getSkills() == null
                        ? ""
                        : profile.getSkills();

        String interests =
                profile.getInterests() == null
                        ? ""
                        : profile.getInterests();

        String combined =
                (skills + "," + interests)
                        .toLowerCase(Locale.ROOT);

        String[] required =
                requiredSkills
                        .toLowerCase(Locale.ROOT)
                        .split("[,\\s]+");

        Set<String> unique =
                new HashSet<>(
                        List.of(required)
                );

        unique.removeIf(
                skill ->
                        skill == null ||
                        skill.trim().length() < 2
        );

        if (unique.isEmpty()) {
            return 0;
        }

        int matches = 0;

        for (String skill : unique) {

            if (combined.contains(
                    skill.trim())) {

                matches++;
            }
        }

        return Math.min(
                100,
                (matches * 100) / unique.size()
        );
    }

    // =========================================================
    // SEND COLLABORATION REQUEST
    // =========================================================

    @PostMapping("/request")
    public Object sendRequest(
            @RequestBody CollaborationRequestData request) {

        User sender =
                userRepository
                        .findById(request.senderId())
                        .orElse(null);

        User receiver =
                userRepository
                        .findById(request.receiverId())
                        .orElse(null);

        Project project =
                projectRepository
                        .findById(request.projectId())
                        .orElse(null);

        if (sender == null ||
                receiver == null ||
                project == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid request data");
        }

        if (sender.getRole() != User.Role.STUDENT ||
                receiver.getRole() != User.Role.STUDENT) {

            return ResponseEntity
                    .badRequest()
                    .body("Only students can collaborate");
        }

        if (sender.getUniversity() == null ||
                receiver.getUniversity() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("University information missing");
        }

        if (!sender.getUniversity()
                .getId()
                .equals(
                        receiver.getUniversity().getId()
                )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Students must belong to the same university"
                    );
        }

        if (project.getUniversity() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Project university is missing"
                    );
        }

        if (!project.getUniversity()
                .getId()
                .equals(
                        sender.getUniversity().getId()
                )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Project belongs to another university"
                    );
        }

        if (project.getCreatedBy()
                .getId()
                .equals(receiver.getId())) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Project owner cannot receive a teammate request"
                    );
        }

        if (projectMemberRepository
                .existsByProjectIdAndStudentId(
                        project.getId(),
                        receiver.getId())) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student is already a project member"
                    );
        }

        if (collaborationRequestRepository
                .existsByProjectIdAndSenderIdAndReceiverIdAndStatus(
                        project.getId(),
                        sender.getId(),
                        receiver.getId(),
                        CollaborationRequest.Status.PENDING
                )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Collaboration request already sent"
                    );
        }

        CollaborationRequest newRequest =
                new CollaborationRequest();

        newRequest.setProject(project);
        newRequest.setSender(sender);
        newRequest.setReceiver(receiver);

        newRequest.setStatus(
                CollaborationRequest.Status.PENDING
        );

        newRequest.setCreatedAt(
                LocalDateTime.now()
        );

        collaborationRequestRepository.save(
                newRequest
        );

        return ResponseEntity.ok(
                "Collaboration request sent successfully"
        );
    }

    // =========================================================
    // RECEIVED REQUESTS
    // =========================================================

    @GetMapping("/received/{userId}")
    public Object getReceivedRequests(
            @PathVariable Long userId) {

        return collaborationRequestRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toRequestResponse)
                .toList();
    }

    // =========================================================
    // SENT REQUESTS
    // =========================================================

    @GetMapping("/sent/{userId}")
    public Object getSentRequests(
            @PathVariable Long userId) {

        return collaborationRequestRepository
                .findBySenderIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toRequestResponse)
                .toList();
    }

    // =========================================================
    // ACCEPT REQUEST
    // =========================================================

    @PutMapping("/request/{requestId}/accept")
    public Object acceptRequest(
            @PathVariable Long requestId) {

        CollaborationRequest request =
                collaborationRequestRepository
                        .findById(requestId)
                        .orElse(null);

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Request not found");
        }

        if (request.getStatus() !=
                CollaborationRequest.Status.PENDING) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Request is no longer pending"
                    );
        }

        Project project =
                request.getProject();

        Long studentId =
                request.getReceiver().getId();

        if (projectMemberRepository
                .existsByProjectIdAndStudentId(
                        project.getId(),
                        studentId)) {

            request.setStatus(
                    CollaborationRequest.Status.ACCEPTED
            );

            collaborationRequestRepository.save(
                    request
            );

            return ResponseEntity.ok(
                    "Student is already a member"
            );
        }

        if (project.getTeamSize() != null) {

            long memberCount =
                    projectMemberRepository
                            .countByProjectId(
                                    project.getId()
                            );

            if (memberCount >=
                    project.getTeamSize()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Project team is already full"
                        );
            }
        }

        ProjectMember member =
                new ProjectMember();

        member.setProject(project);

        member.setStudent(
                request.getReceiver()
        );

        member.setJoinedAt(
                LocalDateTime.now()
        );

        projectMemberRepository.save(member);

        request.setStatus(
                CollaborationRequest.Status.ACCEPTED
        );

        collaborationRequestRepository.save(
                request
        );

        return ResponseEntity.ok(
                "Collaboration request accepted"
        );
    }

    // =========================================================
    // REJECT REQUEST
    // =========================================================

    @PutMapping("/request/{requestId}/reject")
    public Object rejectRequest(
            @PathVariable Long requestId) {

        CollaborationRequest request =
                collaborationRequestRepository
                        .findById(requestId)
                        .orElse(null);

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Request not found"
                    );
        }

        request.setStatus(
                CollaborationRequest.Status.REJECTED
        );

        collaborationRequestRepository.save(
                request
        );

        return ResponseEntity.ok(
                "Collaboration request rejected"
        );
    }

    // =========================================================
    // PROJECT MEMBERS
    // =========================================================

    @GetMapping("/members/{projectId}")
    public Object getProjectMembers(
            @PathVariable Long projectId) {

        return projectMemberRepository
                .findByProjectId(projectId)
                .stream()
                .map(member ->
                        new MemberResponse(
                                member.getStudent().getId(),
                                member.getStudent().getName(),
                                member.getStudent().getEmail()
                        )
                )
                .toList();
    }

    // =========================================================
    // STUDENT ↔ STUDENT CHAT
    // =========================================================

    @GetMapping(
            "/chat/{projectId}/{userId}/{otherUserId}"
    )
    public Object getChat(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @PathVariable Long otherUserId) {

        if (!areAcceptedMembers(
                projectId,
                userId,
                otherUserId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Chat is available only after collaboration is accepted"
                    );
        }

        List<ProjectMessage> messages =
                projectMessageRepository
                        .findConversationMessages(
                                projectId,
                                userId,
                                otherUserId
                        );

        return ResponseEntity.ok(
                messages
                        .stream()
                        .map(this::toChatResponse)
                        .toList()
        );
    }

    // =========================================================
    // SEND CHAT MESSAGE
    // =========================================================

    @PostMapping("/chat")
    public Object sendChatMessage(
            @RequestBody ChatMessageRequest request) {

        if (request.projectId() == null ||
                request.senderId() == null ||
                request.receiverId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Project, sender and receiver are required"
                    );
        }

        if (request.message() == null ||
                request.message().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Message cannot be empty"
                    );
        }

        User sender =
                userRepository
                        .findById(request.senderId())
                        .orElse(null);

        User receiver =
                userRepository
                        .findById(request.receiverId())
                        .orElse(null);

        Project project =
                projectRepository
                        .findById(request.projectId())
                        .orElse(null);

        if (sender == null ||
                receiver == null ||
                project == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid chat information"
                    );
        }

        if (sender.getId()
                .equals(receiver.getId())) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "You cannot message yourself"
                    );
        }

        if (!areAcceptedMembers(
                project.getId(),
                sender.getId(),
                receiver.getId())) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Chat is not available"
                    );
        }

        ProjectMessage message =
                new ProjectMessage();

        message.setProject(project);

        message.setSender(sender);

        message.setReceiver(receiver);

        message.setMessage(
                request.message().trim()
        );

        message.setSentAt(
                LocalDateTime.now()
        );

        ProjectMessage saved =
                projectMessageRepository.save(
                        message
                );

        return ResponseEntity.ok(
                toChatResponse(saved)
        );
    }

    // =========================================================
    // CHECK CHAT PERMISSION
    // =========================================================

    private boolean areAcceptedMembers(
            Long projectId,
            Long firstUserId,
            Long secondUserId) {

        Project project =
                projectRepository
                        .findById(projectId)
                        .orElse(null);

        if (project == null) {
            return false;
        }

        User firstUser =
                userRepository
                        .findById(firstUserId)
                        .orElse(null);

        User secondUser =
                userRepository
                        .findById(secondUserId)
                        .orElse(null);

        if (firstUser == null ||
                secondUser == null) {

            return false;
        }

        if (project.getUniversity() == null ||
                firstUser.getUniversity() == null ||
                secondUser.getUniversity() == null) {

            return false;
        }

        Long universityId =
                project.getUniversity().getId();

        if (!universityId.equals(
                firstUser.getUniversity().getId())) {

            return false;
        }

        if (!universityId.equals(
                secondUser.getUniversity().getId())) {

            return false;
        }

        // Faculty guidance chat
        boolean facultyChat =
                (firstUser.getRole() == User.Role.STUDENT &&
                        secondUser.getRole() == User.Role.FACULTY)
                ||
                (firstUser.getRole() == User.Role.FACULTY &&
                        secondUser.getRole() == User.Role.STUDENT);

        if (facultyChat) {
            return true;
        }

        // Student ↔ Student
        if (firstUser.getRole() != User.Role.STUDENT ||
                secondUser.getRole() != User.Role.STUDENT) {

            return false;
        }

        boolean firstIsOwner =
                project.getCreatedBy() != null &&
                project.getCreatedBy()
                        .getId()
                        .equals(firstUserId);

        boolean secondIsOwner =
                project.getCreatedBy() != null &&
                project.getCreatedBy()
                        .getId()
                        .equals(secondUserId);

        boolean firstIsMember =
                projectMemberRepository
                        .existsByProjectIdAndStudentId(
                                projectId,
                                firstUserId
                        );

        boolean secondIsMember =
                projectMemberRepository
                        .existsByProjectIdAndStudentId(
                                projectId,
                                secondUserId
                        );

        return
                (firstIsOwner || firstIsMember)
                &&
                (secondIsOwner || secondIsMember);
    }

    // =========================================================
    // FACULTY CHAT
    // =========================================================

    @GetMapping(
            "/faculty-chat/{projectId}/{studentId}/{facultyId}"
    )
    public Object getFacultyChat(
            @PathVariable Long projectId,
            @PathVariable Long studentId,
            @PathVariable Long facultyId) {

        User student =
                userRepository
                        .findById(studentId)
                        .orElse(null);

        User faculty =
                userRepository
                        .findById(facultyId)
                        .orElse(null);

        if (student == null ||
                faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "User not found"
                    );
        }

        if (student.getRole() != User.Role.STUDENT) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "First user must be a student"
                    );
        }

        if (faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Second user must be faculty"
                    );
        }

        if (!areAcceptedMembers(
                projectId,
                studentId,
                facultyId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "Faculty chat is not available"
                    );
        }

        List<ProjectMessage> messages =
                projectMessageRepository
                        .findConversationMessages(
                                projectId,
                                studentId,
                                facultyId
                        );

        return ResponseEntity.ok(
                messages
                        .stream()
                        .map(this::toChatResponse)
                        .toList()
        );
    }

    // =========================================================
    // FACULTY GUIDANCE CONTACTS
    //
    // NEW
    //
    // Returns every student who has already exchanged
    // messages with this faculty member.
    // =========================================================

    @GetMapping("/faculty-contacts/{facultyId}")
    public Object getFacultyContacts(
            @PathVariable Long facultyId) {

        User faculty =
                userRepository
                        .findById(facultyId)
                        .orElse(null);

        if (faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Faculty member not found"
                    );
        }

        if (faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "User is not a faculty member"
                    );
        }

        /*
         * We get all messages involving this faculty member
         * and build a unique student/project conversation list.
         */
        List<ProjectMessage> allMessages =
                projectMessageRepository.findAll();

        Map<String, FacultyContactResponse> contacts =
                new LinkedHashMap<>();

        for (ProjectMessage message : allMessages) {

            if (message == null ||
                    message.getSender() == null ||
                    message.getReceiver() == null ||
                    message.getProject() == null) {

                continue;
            }

            Long senderId =
                    message.getSender().getId();

            Long receiverId =
                    message.getReceiver().getId();

            Long facultyIdFromMessage = null;
            Long studentIdFromMessage = null;

            if (facultyId.equals(senderId) &&
                    message.getReceiver().getRole()
                            == User.Role.STUDENT) {

                facultyIdFromMessage = senderId;
                studentIdFromMessage = receiverId;

            } else if (
                    facultyId.equals(receiverId) &&
                    message.getSender().getRole()
                            == User.Role.STUDENT) {

                facultyIdFromMessage = receiverId;
                studentIdFromMessage = senderId;
            }

            if (facultyIdFromMessage == null ||
                    studentIdFromMessage == null) {

                continue;
            }

            User student =
                    userRepository
                            .findById(studentIdFromMessage)
                            .orElse(null);

            if (student == null) {
                continue;
            }

            Long projectId =
                    message.getProject().getId();

            String key =
                    projectId + "-" + studentIdFromMessage;

            contacts.put(
                    key,
                    new FacultyContactResponse(
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            projectId,
                            message.getProject().getTitle(),
                            message.getSentAt()
                    )
            );
        }

        List<FacultyContactResponse> result =
                new ArrayList<>(
                        contacts.values()
                );

        result.sort(
                (a, b) -> {

                    if (a.lastMessageAt() == null &&
                            b.lastMessageAt() == null) {

                        return 0;
                    }

                    if (a.lastMessageAt() == null) {
                        return 1;
                    }

                    if (b.lastMessageAt() == null) {
                        return -1;
                    }

                    return b.lastMessageAt()
                            .compareTo(
                                    a.lastMessageAt()
                            );
                }
        );

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // FACULTY CHAT - USING EXISTING PROJECT
    // =========================================================

    @GetMapping(
            "/faculty-chat/{studentId}/{facultyId}"
    )
    public Object getFacultyChatWithoutProject(
            @PathVariable Long studentId,
            @PathVariable Long facultyId) {

        User student =
                userRepository
                        .findById(studentId)
                        .orElse(null);

        User faculty =
                userRepository
                        .findById(facultyId)
                        .orElse(null);

        if (student == null ||
                faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "User not found"
                    );
        }

        if (student.getRole() != User.Role.STUDENT ||
                faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid student or faculty"
                    );
        }

        List<ProjectMessage> messages =
                projectMessageRepository.findAll()
                        .stream()
                        .filter(message ->
                                message.getSender() != null &&
                                message.getReceiver() != null &&
                                (
                                        (
                                                message.getSender()
                                                        .getId()
                                                        .equals(studentId)
                                                &&
                                                message.getReceiver()
                                                        .getId()
                                                        .equals(facultyId)
                                        )
                                        ||
                                        (
                                                message.getSender()
                                                        .getId()
                                                        .equals(facultyId)
                                                &&
                                                message.getReceiver()
                                                        .getId()
                                                        .equals(studentId)
                                        )
                                )
                        )
                        .sorted(
                                (a, b) ->
                                        a.getSentAt()
                                                .compareTo(
                                                        b.getSentAt()
                                                )
                        )
                        .toList();

        return ResponseEntity.ok(
                messages
                        .stream()
                        .map(this::toChatResponse)
                        .toList()
        );
    }

    // =========================================================
    // SEND FACULTY GUIDANCE MESSAGE
    //
    // NEW
    //
    // This is deliberately separate from student-to-student
    // collaboration logic.
    // =========================================================

    @PostMapping("/faculty-chat")
    public Object sendFacultyChatMessage(
            @RequestBody FacultyChatMessageRequest request) {

        if (request.studentId() == null ||
                request.facultyId() == null ||
                request.message() == null ||
                request.message().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student, faculty and message are required"
                    );
        }

        User student =
                userRepository
                        .findById(request.studentId())
                        .orElse(null);

        User faculty =
                userRepository
                        .findById(request.facultyId())
                        .orElse(null);

        if (student == null ||
                faculty == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student or faculty not found"
                    );
        }

        if (student.getRole() != User.Role.STUDENT) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Sender student ID is invalid"
                    );
        }

        if (faculty.getRole() != User.Role.FACULTY) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Faculty ID is invalid"
                    );
        }

        if (student.getUniversity() == null ||
                faculty.getUniversity() == null ||
                !student.getUniversity()
                        .getId()
                        .equals(
                                faculty.getUniversity().getId()
                        )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student and faculty must belong to the same university"
                    );
        }

        Project project = null;

        /*
         * If frontend supplied a project, use it.
         */
        if (request.projectId() != null) {

            project =
                    projectRepository
                            .findById(request.projectId())
                            .orElse(null);

            if (project == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Project not found"
                        );
            }

            if (project.getUniversity() == null ||
                    !project.getUniversity()
                            .getId()
                            .equals(
                                    student.getUniversity()
                                            .getId()
                            )) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Project belongs to another university"
                        );
            }
        }

        /*
         * If no project was supplied, use an existing project
         * belonging to the student from the first available
         * project message conversation.
         */
        if (project == null) {

            List<ProjectMessage> previousMessages =
                    projectMessageRepository
                            .findAll()
                            .stream()
                            .filter(message ->
                                    message.getProject() != null &&
                                    message.getSender() != null &&
                                    message.getReceiver() != null &&

                                    (
                                            (
                                                    message.getSender()
                                                            .getId()
                                                            .equals(student.getId())
                                                    &&
                                                    message.getReceiver()
                                                            .getId()
                                                            .equals(faculty.getId())
                                            )
                                            ||
                                            (
                                                    message.getSender()
                                                            .getId()
                                                            .equals(faculty.getId())
                                                    &&
                                                    message.getReceiver()
                                                            .getId()
                                                            .equals(student.getId())
                                            )
                                    )
                            )
                            .sorted(
                                    (a, b) ->
                                            b.getSentAt()
                                                    .compareTo(
                                                            a.getSentAt()
                                                    )
                            )
                            .toList();

            if (!previousMessages.isEmpty()) {

                project =
                        previousMessages
                                .get(0)
                                .getProject();
            }
        }

        /*
         * If there is still no project, try to find a project
         * created by the student.
         */
        if (project == null) {

            List<Project> studentProjects =
                    projectRepository
                            .findByCreatedById(
                                    student.getId()
                            );

            if (!studentProjects.isEmpty()) {

                project =
                        studentProjects.get(0);
            }
        }

        /*
         * ProjectMessage requires a project in the current
         * data model, so we need one before saving.
         */
        if (project == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "No project is associated with this guidance conversation"
                    );
        }

        ProjectMessage message =
                new ProjectMessage();

        message.setProject(project);

        message.setSender(student);

        message.setReceiver(faculty);

        message.setMessage(
                request.message().trim()
        );

        message.setSentAt(
                LocalDateTime.now()
        );

        ProjectMessage saved =
                projectMessageRepository.save(
                        message
                );

        return ResponseEntity.ok(
                toChatResponse(saved)
        );
    }

    // =========================================================
    // CONVERT MESSAGE TO RESPONSE
    // =========================================================

    private ChatMessageResponse toChatResponse(
            ProjectMessage message) {

        return new ChatMessageResponse(
                message.getId(),

                message.getSender().getId(),

                message.getSender().getName(),

                message.getReceiver().getId(),

                message.getMessage(),

                message.getSentAt()
        );
    }

    // =========================================================
    // REQUEST RESPONSE
    // =========================================================

    private RequestResponse toRequestResponse(
            CollaborationRequest request) {

        return new RequestResponse(
                request.getId(),

                request.getProject().getId(),

                request.getProject().getTitle(),

                request.getSender().getId(),

                request.getSender().getName(),

                request.getReceiver().getId(),

                request.getReceiver().getName(),

                request.getStatus().name(),

                request.getCreatedAt()
        );
    }

    // =========================================================
    // DTOs
    // =========================================================

    public record CandidateResponse(

            Long id,

            String name,

            String email,

            String department,

            Integer year,

            String skills,

            String interests,

            String bio,

            String achievements,

            String certifications,

            int matchScore

    ) {
    }

    public record CollaborationRequestData(

            Long projectId,

            Long senderId,

            Long receiverId

    ) {
    }

    public record RequestResponse(

            Long id,

            Long projectId,

            String projectTitle,

            Long senderId,

            String senderName,

            Long receiverId,

            String receiverName,

            String status,

            LocalDateTime createdAt

    ) {
    }

    public record MemberResponse(

            Long id,

            String name,

            String email

    ) {
    }

    public record ChatMessageRequest(

            Long projectId,

            Long senderId,

            Long receiverId,

            String message

    ) {
    }

    public record ChatMessageResponse(

            Long id,

            Long senderId,

            String senderName,

            Long receiverId,

            String message,

            LocalDateTime sentAt

    ) {
    }

    // =========================================================
    // NEW: FACULTY CONTACT RESPONSE
    // =========================================================

    public record FacultyContactResponse(

            Long studentId,

            String studentName,

            String studentEmail,

            Long projectId,

            String projectTitle,

            LocalDateTime lastMessageAt

    ) {
    }

    // =========================================================
    // NEW: FACULTY CHAT MESSAGE REQUEST
    // =========================================================

    public record FacultyChatMessageRequest(

            Long studentId,

            Long facultyId,

            Long projectId,

            String message

    ) {
    }
}