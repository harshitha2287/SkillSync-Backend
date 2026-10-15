package com.skillsync.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String organization;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String skills;

    private LocalDate deadline;

    @Column(length = 1000)
    private String registrationLink;

    private String status;

    private LocalDateTime createdAt;


    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;


    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public Opportunity() {
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOrganization() {
        return organization;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getSkills() {
        return skills;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getRegistrationLink() {
        return registrationLink;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public University getUniversity() {
        return university;
    }


    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setRegistrationLink(String registrationLink) {
        this.registrationLink = registrationLink;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setUniversity(University university) {
        this.university = university;
    }
}