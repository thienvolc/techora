package com.techora.user.dto;

import com.techora.user.entity.UserRole;

import java.util.UUID;

public record UserSummary(UUID id, String username, UserRole role) {
}
