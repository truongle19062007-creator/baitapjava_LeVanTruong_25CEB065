package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class ChangePasswordDialog {

    private final ApiService apiService;

    public ChangePasswordDialog(ApiService apiService) {
        this.apiService = apiService;
    }

    public void show() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText(null);

        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Mật khẩu hiện tại");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Mật khẩu mới (tối thiểu 6 ký tự)");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Xác nhận mật khẩu mới");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Mật khẩu hiện tại:"), oldPasswordField);
        grid.addRow(1, new Label("Mật khẩu mới:"), newPasswordField);
        grid.addRow(2, new Label("Xác nhận:"), confirmPasswordField);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                String oldPass = oldPasswordField.getText();
                String newPass = newPasswordField.getText();
                String confirmPass = confirmPasswordField.getText();

                if (!newPass.equals(confirmPass)) {
                    DialogUtil.showError("Lỗi", "Mật khẩu mới và xác nhận không khớp");
                    return null;
                }
                AsyncTask.runVoid(
                        () -> apiService.changePassword(oldPass, newPass),
                        () -> DialogUtil.showInfo("Thành công", "Đổi mật khẩu thành công"),
                        DialogUtil::showError
                );
            }
            return null;
        });

        dialog.showAndWait();
    }
}
