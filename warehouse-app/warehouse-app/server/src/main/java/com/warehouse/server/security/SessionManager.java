package com.warehouse.server.security;

import com.warehouse.server.model.User;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý session/token đăng nhập trong bộ nhớ (in-memory).
 * Thread-safe: dùng ConcurrentHashMap vì server xử lý mỗi client bằng 1 thread riêng,
 * nhiều thread có thể đọc/ghi session đồng thời.
 *
 * Lưu ý: đây là giải pháp phù hợp cho 1 server instance duy nhất (đúng với mô hình
 * ServerSocket trong đề bài). Nếu sau này scale ra nhiều server instance, cần chuyển
 * sang lưu session ở Redis hoặc DB.
 */
public class SessionManager {

    private static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30 phút không hoạt động -> hết hạn
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session createSession(User user) {
        String token = generateToken();
        Session session = new Session(token, user);
        sessions.put(token, session);
        return session;
    }

    /** Trả về session nếu token hợp lệ và chưa hết hạn, ngược lại trả về null. */
    public Session validate(String token) {
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.isExpired(SESSION_TIMEOUT_MILLIS)) {
            sessions.remove(token);
            return null;
        }
        session.touch();
        return session;
    }

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
