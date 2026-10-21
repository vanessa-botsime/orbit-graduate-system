package com.orbit.backend.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String role,
        ApplicationStatus status,
        LocalDate deadline,
        LocalDate appliedOn,
        String notes) {
}
