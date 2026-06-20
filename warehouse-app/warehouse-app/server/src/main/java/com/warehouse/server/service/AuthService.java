package com.warehouse.server.service;

import com.warehouse.server.dao.UserDAO;
import com.warehouse.server.model.User;
import com.warehouse.server.security.PasswordUtil;
import com.warehouse.server.security.Session;
import com.warehouse.server.security.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService(UserDAO userDAO, SessionManager sessionManager) {
        this.userDAO = userDAO;
        this.sessionManager = sessionManager;
    }

    /**
     * Xác thực username/password. Trả về Session nếu hợp lệ.
     * Ném BusinessException với thông báo chung "Sai tên đăng nhập hoặc mật khẩu"
     * trong MỌI trường hợp thất bại (không tồn tại / sai mật khẩu / bị khoá) để tránh
     * lộ thông tin cho kẻ tấn công dò tài khoản (user enumeration).
     */
    public Session login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Tên đăng nhập và mật khẩu không được để trống", "VALIDATION_ERROR");
        }
        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());
            if (userOpt.isEmpty()) {
                throw new BusinessException("Sai tên đăng nhập hoặc mật khẩu", "AUTH_INVALID");
            }
            User user = userOpt.get();
            if (!user.isActive()) {
                throw new BusinessException("Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên", "AUTH_DISABLED");
            }
            if (!PasswordUtil.verify(password, user.getPasswordHash())) {
                throw new BusinessException("Sai tên đăng nhập hoặc mật khẩu", "AUTH_INVALID");
            }
            return sessionManager.createSession(user);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi đăng nhập", e);
        }
    }

    public void logout(String token) {
        sessionManager.invalidate(token);
    }

    /** Kiểm tra token hợp lệ, trả về Session. Ném BusinessException nếu không hợp lệ/hết hạn. */
    public Session requireSession(String token) {
        Session session = sessionManager.validate(token);
        if (session == null) {
            throw new BusinessException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại", "SESSION_INVALID");
        }
        return session;
    }

    /** Kiểm tra session có quyền role tương ứng không (ví dụ chỉ ADMIN mới được quản lý user). */
    public void requireRole(Session session, String... allowedRoles) {
        String role = session.getUser().getRole();
        for (String r : allowedRoles) {
            if (r.equals(role)) {
                return;
            }
        }
        throw new BusinessException("Bạn không có quyền thực hiện hành động này", "FORBIDDEN");
    }

    public void changePassword(Session session, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự", "VALIDATION_ERROR");
        }
        try {
            User user = session.getUser();
            if (!PasswordUtil.verify(oldPassword, user.getPasswordHash())) {
                throw new BusinessException("Mật khẩu hiện tại không đúng", "AUTH_INVALID");
            }
            String newHash = PasswordUtil.hash(newPassword);
            userDAO.updatePassword(user.getId(), newHash);
            user.setPasswordHash(newHash); // cập nhật luôn trong session đang giữ
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi đổi mật khẩu", e);
        }
    }
}
