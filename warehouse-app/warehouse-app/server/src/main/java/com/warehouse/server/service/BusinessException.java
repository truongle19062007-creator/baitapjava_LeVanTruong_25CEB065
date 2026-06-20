package com.warehouse.server.service;

/**
 * Exception nghiệp vụ - dùng để báo các lỗi có thể đoán trước và cần hiển thị
 * thông báo rõ ràng cho người dùng (VD: "Tồn kho không đủ", "Tài khoản không tồn tại").
 * Phân biệt với RuntimeException/SQLException thông thường (lỗi hệ thống, không lộ chi tiết cho client).
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
