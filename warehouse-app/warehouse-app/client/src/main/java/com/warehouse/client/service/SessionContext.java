package com.warehouse.client.service;

import com.warehouse.shared.dto.UserDTO;

/** Lưu trạng thái phiên đăng nhập hiện tại trên client (token + user). Singleton đơn giản. */
public class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String token;
    private UserDTO currentUser;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return token != null && currentUser != null;
    }

    public boolean hasRole(String... roles) {
        if (currentUser == null) return false;
        for (String r : roles) {
            if (r.equals(currentUser.getRole())) return true;
        }
        return false;
    }

    public void clear() {
        token = null;
        currentUser = null;
    }
}
