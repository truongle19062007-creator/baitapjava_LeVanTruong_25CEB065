package com.warehouse.server.security;

import com.warehouse.server.model.User;

import java.time.Instant;

/** Đại diện 1 phiên đăng nhập đang hoạt động, gắn với 1 token. */
public class Session {
    private final String token;
    private final User user;
    private final Instant createdAt;
    private volatile Instant lastAccessAt;

    public Session(String token, User user) {
        this.token = token;
        this.user = user;
        this.createdAt = Instant.now();
        this.lastAccessAt = Instant.now();
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessAt() {
        return lastAccessAt;
    }

    public void touch() {
        this.lastAccessAt = Instant.now();
    }

    public boolean isExpired(long timeoutMillis) {
        return Instant.now().toEpochMilli() - lastAccessAt.toEpochMilli() > timeoutMillis;
    }
}
