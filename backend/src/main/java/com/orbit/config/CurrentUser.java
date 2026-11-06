package com.orbit.config;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
