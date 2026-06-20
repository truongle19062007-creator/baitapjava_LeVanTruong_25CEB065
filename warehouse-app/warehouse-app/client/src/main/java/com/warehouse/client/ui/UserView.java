package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.UserDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class UserView {

    private static final List<String> ROLES = List.of("ADMIN", "MANAGER", "STAFF");

    private final ApiService apiService;
    private final TableView<UserDTO> table = new TableView<>();
    private final ObservableList<UserDTO> data = FXCollections.observableArrayList();

    public UserView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Quản lý Người dùng");
        title.getStyleClass().add("section-title");

        Button addBtn = new Button("+ Thêm người dùng");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showCreateDialog());

        Button refreshBtn = new Button("Tải lại");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> loadData());

        HBox toolbar = new HBox(10, addBtn, refreshBtn);
        setupTable();

        VBox box = new VBox(15, title, toolbar, table);
        loadData();
        return box;
    }

    private void setupTable() {
        TableColumn<UserDTO, String> usernameCol = new TableColumn<>("Tên đăng nhập");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);

        TableColumn<UserDTO, String> fullNameCol = new TableColumn<>("Họ tên");
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fullNameCol.setPrefWidth(200);

        TableColumn<UserDTO, String> roleCol = new TableColumn<>("Vai trò");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(100);

        TableColumn<UserDTO, Boolean> activeCol = new TableColumn<>("Kích hoạt");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(80);

        TableColumn<UserDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setPrefWidth(260);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button resetBtn = new Button("Đặt lại MK");
            private final Button deleteBtn = new Button("Xoá");
            {
                editBtn.getStyleClass().add("secondary-button");
                resetBtn.getStyleClass().add("secondary-button");
                deleteBtn.getStyleClass().add("danger-button");
                editBtn.setOnAction(e -> showEditDialog(getTableView().getItems().get(getIndex())));
                resetBtn.setOnAction(e -> showResetPasswordDialog(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(6, editBtn, resetBtn, deleteBtn));
            }
        });

        table.getColumns().addAll(usernameCol, fullNameCol, roleCol, activeCol, actionCol);
        table.setItems(data);
        table.setPrefHeight(500);
    }

    private void loadData() {
        AsyncTask.run(apiService::listUsers, list -> data.setAll(list), DialogUtil::showError);
    }

    private void showCreateDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thêm người dùng");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập (3-50 ký tự, chữ/số/_/.)");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu (tối thiểu 6 ký tự)");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Họ và tên");
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(ROLES));
        roleCombo.setValue("STAFF");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Tên đăng nhập:"), usernameField);
        grid.addRow(1, new Label("Mật khẩu:"), passwordField);
        grid.addRow(2, new Label("Họ tên:"), fullNameField);
        grid.addRow(3, new Label("Vai trò:"), roleCombo);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                AsyncTask.runVoid(
                        () -> apiService.createUser(usernameField.getText().trim(), passwordField.getText(),
                                fullNameField.getText().trim(), roleCombo.getValue()),
                        () -> { DialogUtil.showInfo("Thành công", "Tạo người dùng thành công"); loadData(); },
                        DialogUtil::showError);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showEditDialog(UserDTO user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sửa người dùng: " + user.getUsername());

        TextField fullNameField = new TextField(user.getFullName());
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(ROLES));
        roleCombo.setValue(user.getRole());
        CheckBox activeCheck = new CheckBox("Kích hoạt tài khoản");
        activeCheck.setSelected(user.isActive());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Họ tên:"), fullNameField);
        grid.addRow(1, new Label("Vai trò:"), roleCombo);
        grid.addRow(2, new Label(""), activeCheck);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                AsyncTask.runVoid(
                        () -> apiService.updateUser(user.getId(), fullNameField.getText().trim(),
                                roleCombo.getValue(), activeCheck.isSelected()),
                        () -> { DialogUtil.showInfo("Thành công", "Cập nhật thành công"); loadData(); },
                        DialogUtil::showError);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showResetPasswordDialog(UserDTO user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đặt lại mật khẩu cho: " + user.getUsername());

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Mật khẩu mới (tối thiểu 6 ký tự)");

        VBox content = new VBox(10, new Label("Mật khẩu mới:"), newPasswordField);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        ButtonType saveType = new ButtonType("Đặt lại", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                AsyncTask.runVoid(
                        () -> apiService.resetUserPassword(user.getId(), newPasswordField.getText()),
                        () -> DialogUtil.showInfo("Thành công", "Đặt lại mật khẩu thành công"),
                        DialogUtil::showError);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleDelete(UserDTO user) {
        if (!DialogUtil.confirm("Xác nhận xoá", "Xoá người dùng '" + user.getUsername() + "'?")) {
            return;
        }
        AsyncTask.runVoid(
                () -> apiService.deleteUser(user.getId()),
                () -> { DialogUtil.showInfo("Thành công", "Đã xoá người dùng"); loadData(); },
                DialogUtil::showError);
    }
}
