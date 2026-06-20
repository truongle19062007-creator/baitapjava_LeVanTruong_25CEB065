package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.service.SessionContext;
import com.warehouse.client.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout chính của ứng dụng sau khi đăng nhập.
 * Sidebar bên trái để chuyển đổi giữa các module (Sản phẩm, Nhập kho, Xuất kho, Tồn kho, Người dùng).
 * Content area ở giữa sẽ thay đổi tuỳ theo module được chọn.
 */
public class MainView {

    private final ApiService apiService;
    private final Stage primaryStage;
    private final BorderPane root = new BorderPane();
    private final Map<String, Button> sidebarButtons = new LinkedHashMap<>();

    public MainView(ApiService apiService, Stage primaryStage) {
        this.apiService = apiService;
        this.primaryStage = primaryStage;
    }

    public Parent getView() {
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        showModule("PRODUCT"); // module mặc định khi vào app
        return root;
    }

    private HBox buildTopBar() {
        Label welcomeLabel = new Label("Xin chào, " + SessionContext.getInstance().getCurrentUser().getFullName() +
                " (" + SessionContext.getInstance().getCurrentUser().getRole() + ")");
        welcomeLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button changePasswordBtn = new Button("Đổi mật khẩu");
        changePasswordBtn.getStyleClass().add("secondary-button");
        changePasswordBtn.setOnAction(e -> new ChangePasswordDialog(apiService).show());

        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.getStyleClass().add("danger-button");
        logoutBtn.setOnAction(e -> handleLogout());

        HBox topBar = new HBox(12, welcomeLabel, spacer, changePasswordBtn, logoutBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("topbar");
        return topBar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        addSidebarButton(sidebar, "PRODUCT", "📦  Sản phẩm");
        addSidebarButton(sidebar, "CATEGORY", "🏷  Danh mục");
        addSidebarButton(sidebar, "SUPPLIER", "🚚  Nhà cung cấp");
        addSidebarButton(sidebar, "IMPORT", "⬇  Nhập kho");
        addSidebarButton(sidebar, "EXPORT", "⬆  Xuất kho");
        addSidebarButton(sidebar, "INVENTORY", "📊  Tồn kho");

        if (SessionContext.getInstance().hasRole("ADMIN")) {
            addSidebarButton(sidebar, "USER", "👤  Người dùng");
        }

        return sidebar;
    }

    private void addSidebarButton(VBox sidebar, String moduleKey, String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showModule(moduleKey));
        sidebarButtons.put(moduleKey, btn);
        sidebar.getChildren().add(btn);
    }

    private void showModule(String moduleKey) {
        // Cập nhật trạng thái active của sidebar button
        sidebarButtons.forEach((key, btn) ->
                btn.getStyleClass().setAll(key.equals(moduleKey) ? "sidebar-button-active" : "sidebar-button"));

        Parent view = switch (moduleKey) {
            case "PRODUCT" -> new ProductView(apiService).getView();
            case "CATEGORY" -> new CategoryView(apiService).getView();
            case "SUPPLIER" -> new SupplierView(apiService).getView();
            case "IMPORT" -> new ImportView(apiService).getView();
            case "EXPORT" -> new ExportView(apiService).getView();
            case "INVENTORY" -> new InventoryView(apiService).getView();
            case "USER" -> new UserView(apiService).getView();
            default -> new Label("Không tìm thấy module: " + moduleKey);
        };

        VBox contentWrapper = new VBox(view);
        contentWrapper.setPadding(new Insets(20));
        root.setCenter(contentWrapper);
    }

    private void handleLogout() {
        if (!DialogUtil.confirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?")) {
            return;
        }
        try {
            apiService.logout();
        } catch (Exception ignored) {
            // Nếu lỗi mạng khi gọi logout, vẫn cho phép về màn hình đăng nhập ở client
        }
        SessionContext.getInstance().clear();
        apiService.disconnect();

        LoginView loginView = new LoginView(apiService, primaryStage);
        javafx.scene.Scene scene = new javafx.scene.Scene(loginView.getView(), 420, 480);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Quản lý kho - Đăng nhập");
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
    }
}
