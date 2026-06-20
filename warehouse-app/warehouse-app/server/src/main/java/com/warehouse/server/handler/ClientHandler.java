package com.warehouse.server.handler;

import com.warehouse.shared.protocol.Request;
import com.warehouse.shared.protocol.Response;
import com.warehouse.shared.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý 1 kết nối client. Mỗi client được Server giao cho 1 thread riêng (xem ServerMain),
 * nên instance này chỉ phục vụ duy nhất 1 Socket trong suốt vòng đời của nó.
 *
 * Giao thức: đọc từng dòng (newline-delimited JSON) từ client, parse thành Request,
 * giao cho RequestRouter xử lý, rồi viết Response (1 dòng JSON) trở lại.
 * Vòng lặp tiếp tục cho tới khi client đóng kết nối hoặc xảy ra lỗi I/O.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestRouter requestRouter;

    public ClientHandler(Socket socket, RequestRouter requestRouter) {
        this.socket = socket;
        this.requestRouter = requestRouter;
    }

    @Override
    public void run() {
        String clientInfo = socket.getRemoteSocketAddress().toString();
        System.out.println("[ClientHandler] Client kết nối: " + clientInfo);

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Response response;
                try {
                    Request request = JsonUtil.fromJson(line, Request.class);
                    if (request == null) {
                        response = Response.fail("Request không hợp lệ (không parse được JSON)", "BAD_REQUEST");
                    } else {
                        response = requestRouter.handle(request);
                    }
                } catch (Exception e) {
                    // Bắt mọi exception ở đây để 1 request lỗi không làm chết cả kết nối (thread).
                    System.err.println("[ClientHandler] Lỗi xử lý request từ " + clientInfo + ": " + e.getMessage());
                    response = Response.fail("Request không hợp lệ hoặc lỗi xử lý", "BAD_REQUEST");
                }

                writer.println(JsonUtil.toJson(response));
                writer.flush();
            }
        } catch (IOException e) {
            System.out.println("[ClientHandler] Mất kết nối với client " + clientInfo + ": " + e.getMessage());
        } finally {
            closeQuietly();
            System.out.println("[ClientHandler] Client đã đóng kết nối: " + clientInfo);
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
