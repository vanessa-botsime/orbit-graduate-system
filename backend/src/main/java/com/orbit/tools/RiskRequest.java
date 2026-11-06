package com.orbit.tools;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RiskRequest(
        @Size(max = 200) String company,
        @Size(max = 255) String contactEmail,
        @Size(max = 1000) String websiteUrl,
        @Size(max = 20000) String postingText,
        UUID applicationId) {
}
