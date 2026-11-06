package com.orbit.tools;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MatchRequest(
        @NotBlank @Size(max = 30000) String cvText,
        @NotBlank @Size(max = 20000) String jobDescription,
        UUID applicationId) {
}
