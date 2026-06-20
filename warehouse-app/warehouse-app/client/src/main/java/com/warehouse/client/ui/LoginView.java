package com.warehouse.client.ui;

import com.warehouse.client.MainApp;
import com.warehouse.client.service.ApiService;
import com.warehouse.client.service.SessionContext;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.LoginResultDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Màn hình đăng nhập. Kết nối tới server được thực hiện ngay khi người dùng bấm "Đăng nhập". */
public class LoginView {

    private final ApiService apiService;
    private final Stage primaryStage;

    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label statusLabel;

    public LoginView(ApiService apiService, Stage primaryStage) {
        this.apiService = apiService;
        this.primaryStage = primaryStage;
    }

    public Parent getView() {
        Label title = new Label("QUẢN LÝ KHO");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Đăng nhập để tiếp tục");
        subtitle.getStyleClass().add("subtitle-label");

        usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        usernameField.setPrefHeight(38);

        passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");
        passwordField.setPrefHeight(38);
        passwordField.setOnAction(e -> handleLogin());

        loginButton = new Button("ĐĂNG NHẬP");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #e53935;");
        statusLabel.setWrapText(true);

        VBox box = new VBox(14, title, subtitle, new Separator(), usernameField, passwordField, loginButton, statusLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("login-box");
        box.setMaxWidth(340);

        VBox wrapper = new VBox(box);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPrefSize(420, 480);
        return wrapper;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        statusLabel.setText("");
        loginButton.setDisable(true);
        loginButton.setText("ĐANG ĐĂNG NHẬP...");

        AsyncTask.run(
                () -> {
                    if (!apiService.isConnected()) {
                        apiService.connect();
                    }
                    return apiService.login(username, password);
                },
                (LoginResultDTO result) -> {
                    loginButton.setDisable(false);
                    loginButton.setText("ĐĂNG NHẬP");
                    SessionContext.getInstance().setToken(result.getToken());
                    SessionContext.getInstance().setCurrentUser(result.getUser());
                    openMainView();
                },
                (Throwable error) -> {
                    loginButton.setDisable(false);
                    loginButton.setText("ĐĂNG NHẬP");
                    String msg = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                    statusLabel.setText(msg != null ? msg : "Không thể kết nối tới server");
                }
        );
    }

    private void openMainView() {
        MainView mainView = new MainView(apiService, primaryStage);
        Scene scene = new Scene(mainView.getView(), 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Quản lý kho - " + SessionContext.getInstance().getCurrentUser().getFullName());
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
    }
}
