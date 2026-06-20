package com.warehouse.client.util;

import com.warehouse.client.service.ApiException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class DialogUtil {

    private DialogUtil() {
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Hiển thị lỗi, tự nhận diện nếu là ApiException (lỗi nghiệp vụ từ server) để hiển thị message gọn. */
    public static void showError(Throwable t) {
        String message;
        if (t instanceof ApiException apiEx) {
            message = apiEx.getMessage();
        } else if (t.getCause() instanceof ApiException apiEx) {
            message = apiEx.getMessage();
        } else {
            message = "Đã có lỗi xảy ra: " + (t.getMessage() != null ? t.getMessage() : t.toString());
        }
        showError("Lỗi", message);
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
