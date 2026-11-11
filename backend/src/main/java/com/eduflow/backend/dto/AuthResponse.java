package com.eduflow.backend.dto;

import com.eduflow.backend.entity.Role;

public record AuthResponse(String token, Long userId, String fullName, String email, Role role) {
}
