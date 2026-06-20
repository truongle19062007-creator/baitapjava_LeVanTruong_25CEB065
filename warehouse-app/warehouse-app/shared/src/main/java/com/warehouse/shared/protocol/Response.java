package com.warehouse.shared.protocol;

/**
 * Envelope chung cho mọi response gửi từ Server -> Client qua Socket.
 * Định dạng truyền tải: 1 dòng JSON, kết thúc bằng ký tự '\n'.
 */
public class Response {

    private boolean success;
    private String message;     // Thông báo lỗi hoặc thông báo thành công (hiển thị cho user)
    private String errorCode;   // Mã lỗi để client xử lý logic (VD: "AUTH_INVALID", "VALIDATION_ERROR")
    private String data;        // JSON string của DTO kết quả (list sản phẩm, thông tin user, v.v.)

    public Response() {
    }

    public Response(boolean success, String message, String errorCode, String data) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
    }

    public static Response ok(String message, String data) {
        return new Response(true, message, null, data);
    }

    public static Response ok(String message) {
        return new Response(true, message, null, null);
    }

    public static Response fail(String message, String errorCode) {
        return new Response(false, message, errorCode, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
