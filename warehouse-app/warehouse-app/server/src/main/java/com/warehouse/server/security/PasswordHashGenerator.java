package com.warehouse.server.security;

/**
 * Công cụ chạy thủ công để tạo BCrypt hash cho mật khẩu,
 * dùng khi cần tạo tài khoản admin đầu tiên trực tiếp trong DB.
 *
 * Cách dùng:
 *   mvn exec:java -Dexec.mainClass="com.warehouse.server.security.PasswordHashGenerator" -Dexec.args="Admin@123"
 * hoặc chạy trực tiếp main() từ IDE với argument là mật khẩu cần hash.
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Cách dùng: PasswordHashGenerator <mật khẩu cần hash>");
            return;
        }
        String plain = args[0];
        String hash = PasswordUtil.hash(plain);
        System.out.println("Password : " + plain);
        System.out.println("Hash     : " + hash);
        System.out.println();
        System.out.println("UPDATE users SET password_hash = '" + hash + "' WHERE username = 'admin';");
    }
}
