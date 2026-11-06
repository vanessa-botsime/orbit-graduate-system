package com.orbit.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id, String company, String roleTitle, String jobUrl, String jobDescription,
        ApplicationStatus status, LocalDate deadline, String notes,
        Integer matchScore, String riskLevel, Instant createdAt, Instant updatedAt) {

    public static ApplicationResponse from(JobApplication a) {
        return new ApplicationResponse(a.getId(), a.getCompany(), a.getRoleTitle(), a.getJobUrl(),
                a.getJobDescription(), a.getStatus(), a.getDeadline(), a.getNotes(),
                a.getMatchScore(), a.getRiskLevel(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
