package com.warehouse.shared.dto;

import java.time.LocalDateTime;

/**
 * DTO người dùng truyền qua mạng. KHÔNG bao giờ chứa password gốc hoặc password hash
 * khi gửi từ Server -> Client (trường passwordHash chỉ dùng nội bộ Server).
 */
public class UserDTO {

    private Long id;
    private String username;
    private String fullName;
    private String role;       // ADMIN, MANAGER, STAFF
    private boolean active;
    private LocalDateTime createdAt;

    public UserDTO() {
    }

    public UserDTO(Long id, String username, String fullName, String role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
