package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.SupplierDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SupplierView {

    private final ApiService apiService;
    private final TableView<SupplierDTO> table = new TableView<>();
    private final ObservableList<SupplierDTO> data = FXCollections.observableArrayList();

    public SupplierView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Quản lý Nhà cung cấp");
        title.getStyleClass().add("section-title");

        Button addBtn = new Button("+ Thêm nhà cung cấp");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showFormDialog(null));

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
        TableColumn<SupplierDTO, String> nameCol = new TableColumn<>("Tên nhà cung cấp");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<SupplierDTO, String> phoneCol = new TableColumn<>("Số điện thoại");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(130);

        TableColumn<SupplierDTO, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(180);

        TableColumn<SupplierDTO, String> addressCol = new TableColumn<>("Địa chỉ");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setPrefWidth(220);

        TableColumn<SupplierDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setPrefWidth(160);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button deleteBtn = new Button("Xoá");
            {
                editBtn.getStyleClass().add("secondary-button");
                deleteBtn.getStyleClass().add("danger-button");
                editBtn.setOnAction(e -> showFormDialog(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(8, editBtn, deleteBtn));
            }
        });

        table.getColumns().addAll(nameCol, phoneCol, emailCol, addressCol, actionCol);
        table.setItems(data);
        table.setPrefHeight(500);
    }

    private void loadData() {
        AsyncTask.run(apiService::listSuppliers, list -> data.setAll(list), DialogUtil::showError);
    }

    private void showFormDialog(SupplierDTO existing) {
        boolean isEdit = existing != null;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Sửa nhà cung cấp" : "Thêm nhà cung cấp");

        TextField nameField = new TextField(isEdit ? existing.getName() : "");
        TextField phoneField = new TextField(isEdit ? existing.getPhone() : "");
        TextField emailField = new TextField(isEdit ? existing.getEmail() : "");
        TextField addressField = new TextField(isEdit ? existing.getAddress() : "");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Tên:"), nameField);
        grid.addRow(1, new Label("SĐT:"), phoneField);
        grid.addRow(2, new Label("Email:"), emailField);
        grid.addRow(3, new Label("Địa chỉ:"), addressField);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    DialogUtil.showError("Lỗi", "Tên nhà cung cấp không được để trống");
                    return null;
                }
                String phone = phoneField.getText();
                String email = emailField.getText();
                String address = addressField.getText();
                if (isEdit) {
                    AsyncTask.runVoid(
                            () -> apiService.updateSupplier(existing.getId(), name, phone, email, address),
                            () -> { DialogUtil.showInfo("Thành công", "Cập nhật thành công"); loadData(); },
                            DialogUtil::showError);
                } else {
                    AsyncTask.runVoid(
                            () -> apiService.createSupplier(name, phone, email, address),
                            () -> { DialogUtil.showInfo("Thành công", "Tạo nhà cung cấp thành công"); loadData(); },
                            DialogUtil::showError);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleDelete(SupplierDTO supplier) {
        if (!DialogUtil.confirm("Xác nhận xoá", "Xoá nhà cung cấp '" + supplier.getName() + "'?")) {
            return;
        }
        AsyncTask.runVoid(
                () -> apiService.deleteSupplier(supplier.getId()),
                () -> { DialogUtil.showInfo("Thành công", "Đã xoá nhà cung cấp"); loadData(); },
                DialogUtil::showError);
    }
}
