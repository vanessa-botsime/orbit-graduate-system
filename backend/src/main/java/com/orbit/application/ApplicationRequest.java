package com.orbit.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 200) String company,
        @NotBlank @Size(max = 200) String roleTitle,
        @Size(max = 1000) String jobUrl,
        @Size(max = 20000) String jobDescription,
        ApplicationStatus status,
        LocalDate deadline,
        @Size(max = 5000) String notes) {
}

