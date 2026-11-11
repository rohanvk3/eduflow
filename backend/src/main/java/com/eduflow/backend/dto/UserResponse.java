package com.eduflow.backend.dto;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import java.time.Instant;

public record UserResponse(Long id, String fullName, String email, Role role, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
