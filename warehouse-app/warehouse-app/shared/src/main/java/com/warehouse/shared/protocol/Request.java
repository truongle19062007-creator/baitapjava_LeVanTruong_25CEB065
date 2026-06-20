package com.warehouse.shared.protocol;

/**
 * Envelope chung cho mọi request gửi từ Client -> Server qua Socket.
 * Định dạng truyền tải: 1 dòng JSON, kết thúc bằng ký tự '\n'.
 *
 * action: tên hành động, dạng "MODULE.OPERATION", ví dụ:
 *   AUTH.LOGIN, PRODUCT.LIST, PRODUCT.CREATE, IMPORT.CREATE, EXPORT.CREATE, INVENTORY.LIST ...
 * token: session token nhận được sau khi đăng nhập (null nếu chưa đăng nhập / đang login)
 * payload: dữ liệu JSON tuỳ theo action, được Gson serialize sẵn thành chuỗi
 *          (giữ dạng String để tránh phải biết trước kiểu cụ thể ở lớp envelope)
 */
public class Request {

    private String action;
    private String token;
    private String payload; // JSON string của DTO tương ứng với action

    public Request() {
    }

    public Request(String action, String token, String payload) {
        this.action = action;
        this.token = token;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
