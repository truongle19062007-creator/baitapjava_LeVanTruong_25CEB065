package com.warehouse.client.service;

/** Ném ra khi server trả về Response với success=false (lỗi nghiệp vụ, không phải lỗi mạng). */
public class ApiException extends RuntimeException {
    private final String errorCode;

    public ApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
