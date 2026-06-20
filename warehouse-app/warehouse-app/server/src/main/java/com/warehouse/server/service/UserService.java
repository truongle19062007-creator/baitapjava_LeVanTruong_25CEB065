package com.warehouse.server.service;

import com.warehouse.server.dao.UserDAO;
import com.warehouse.server.model.User;
import com.warehouse.server.security.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "MANAGER", "STAFF");

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public List<User> listAll() {
        try {
            return userDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách người dùng", e);
        }
    }

    public User create(String username, String plainPassword, String fullName, String role) {
        validateUsername(username);
        validateRole(role);
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự", "VALIDATION_ERROR");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Họ tên không được để trống", "VALIDATION_ERROR");
        }
        try {
            Optional<User> existing = userDAO.findByUsername(username.trim());
            if (existing.isPresent()) {
                throw new BusinessException("Tên đăng nhập đã tồn tại", "CONFLICT");
            }
            User user = new User();
            user.setUsername(username.trim());
            user.setPasswordHash(PasswordUtil.hash(plainPassword));
            user.setFullName(fullName.trim());
            user.setRole(role);
            user.setActive(true);
            return userDAO.insert(user);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi tạo người dùng", e);
        }
    }

    public void update(Long id, String fullName, String role, boolean active) {
        validateRole(role);
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Họ tên không được để trống", "VALIDATION_ERROR");
        }
        try {
            User user = userDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng", "NOT_FOUND"));
            user.setFullName(fullName.trim());
            user.setRole(role);
            user.setActive(active);
            userDAO.update(user);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi cập nhật người dùng", e);
        }
    }

    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự", "VALIDATION_ERROR");
        }
        try {
            userDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng", "NOT_FOUND"));
            userDAO.updatePassword(id, PasswordUtil.hash(newPassword));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi đặt lại mật khẩu", e);
        }
    }

    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("Không thể tự xoá chính tài khoản đang đăng nhập", "VALIDATION_ERROR");
        }
        try {
            userDAO.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi xoá người dùng", e);
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Tên đăng nhập không được để trống", "VALIDATION_ERROR");
        }
        if (!username.matches("^[a-zA-Z0-9_.]{3,50}$")) {
            throw new BusinessException("Tên đăng nhập chỉ gồm chữ, số, gạch dưới, dấu chấm (3-50 ký tự)", "VALIDATION_ERROR");
        }
    }

    private void validateRole(String role) {
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new BusinessException("Vai trò không hợp lệ (ADMIN, MANAGER hoặc STAFF)", "VALIDATION_ERROR");
        }
    }
}
