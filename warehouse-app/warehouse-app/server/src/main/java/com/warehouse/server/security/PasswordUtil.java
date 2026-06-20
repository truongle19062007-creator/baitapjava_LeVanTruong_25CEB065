package com.warehouse.server.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Hash và kiểm tra mật khẩu bằng BCrypt.
 * KHÔNG bao giờ lưu hoặc log mật khẩu plaintext.
 */
public final class PasswordUtil {

    private static final int COST_FACTOR = 12; // độ khó hash, càng cao càng chậm nhưng an toàn hơn

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plainPassword.toCharArray());
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
