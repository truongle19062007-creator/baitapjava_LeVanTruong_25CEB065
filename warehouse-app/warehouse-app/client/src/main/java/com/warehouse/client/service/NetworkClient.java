package com.warehouse.client.service;

import com.warehouse.shared.protocol.Request;
import com.warehouse.shared.protocol.Response;
import com.warehouse.shared.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Quản lý kết nối Socket duy nhất tới Server.
 *
 * Thiết kế: 1 client app giữ 1 Socket xuyên suốt phiên làm việc (connect 1 lần lúc mở app
 * hoặc lúc đăng nhập, giữ kết nối, gửi nhiều request/response qua lại, disconnect khi đóng app).
 *
 * Lưu ý quan trọng: các phương thức gửi/nhận của lớp này KHÔNG thread-safe khi gọi đồng thời
 * từ nhiều thread (vì dùng chung 1 InputStream/OutputStream). Tầng gọi (ApiService) phải
 * đồng bộ hoá (synchronized) để đảm bảo tại 1 thời điểm chỉ có 1 request đang "in-flight".
 */
public class NetworkClient {

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Gửi 1 Request và đợi đúng 1 dòng Response trả về (giao thức là request-response đồng bộ,
     * tuần tự theo dòng). Method này synchronized để đảm bảo không có 2 thread cùng gửi/đọc
     * trên cùng 1 socket tại 1 thời điểm (tránh lẫn dữ liệu giữa các response).
     */
    public synchronized Response send(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối tới server hoặc kết nối đã bị đóng");
        }
        String json = JsonUtil.toJson(request);
        writer.println(json);
        writer.flush();

        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Server đã đóng kết nối");
        }
        Response response = JsonUtil.fromJson(line, Response.class);
        if (response == null) {
            throw new IOException("Không parse được phản hồi từ server");
        }
        return response;
    }

    public synchronized void disconnect() {
        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {
        }
        try {
            if (reader != null) reader.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
    }
}
