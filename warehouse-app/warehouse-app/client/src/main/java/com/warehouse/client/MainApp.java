package com.warehouse.client;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.service.NetworkClient;
import com.warehouse.client.service.SessionContext;
import com.warehouse.client.ui.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point của JavaFX Client Application.
 *
 * Lưu ý: việc kết nối tới server (NetworkClient.connect()) được thực hiện ngay tại màn hình
 * đăng nhập (LoginView), không phải ở đây, để có thể hiển thị lỗi kết nối rõ ràng cho người dùng
 * (host/port sai, server chưa chạy...) ngay từ đầu, thay vì app bị treo khi khởi động.
 */
public class MainApp extends Application {

    public static final String SERVER_HOST = System.getProperty("warehouse.server.host", "localhost");
    public static final int SERVER_PORT = Integer.parseInt(System.getProperty("warehouse.server.port", "9999"));

    private ApiService apiService;

    @Override
    public void start(Stage primaryStage) {
        NetworkClient networkClient = new NetworkClient(SERVER_HOST, SERVER_PORT);
        this.apiService = new ApiService(networkClient);

        primaryStage.setTitle("Quản lý kho - Đăng nhập");
        primaryStage.setResizable(false);

        LoginView loginView = new LoginView(apiService, primaryStage);
        Scene scene = new Scene(loginView.getView(), 420, 480);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            apiService.disconnect();
            SessionContext.getInstance().clear();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
