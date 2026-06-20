package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.CategoryDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CategoryView {

    private final ApiService apiService;
    private final TableView<CategoryDTO> table = new TableView<>();
    private final ObservableList<CategoryDTO> data = FXCollections.observableArrayList();

    public CategoryView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Quản lý Danh mục sản phẩm");
        title.getStyleClass().add("section-title");

        Button addBtn = new Button("+ Thêm danh mục");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showFormDialog(null));

        Button refreshBtn = new Button("Tải lại");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> loadData());

        HBox toolbar = new HBox(10, addBtn, refreshBtn);

        setupTable();

        VBox box = new VBox(15, title, toolbar, table);
        box.setPadding(new Insets(0));
        loadData();
        return box;
    }

    private void setupTable() {
        TableColumn<CategoryDTO, String> nameCol = new TableColumn<>("Tên danh mục");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);

        TableColumn<CategoryDTO, String> descCol = new TableColumn<>("Mô tả");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(350);

        TableColumn<CategoryDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setPrefWidth(180);
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

        table.getColumns().addAll(nameCol, descCol, actionCol);
        table.setItems(data);
        table.setPrefHeight(500);
    }

    private void loadData() {
        AsyncTask.run(apiService::listCategories, list -> data.setAll(list), DialogUtil::showError);
    }

    private void showFormDialog(CategoryDTO existing) {
        boolean isEdit = existing != null;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Sửa danh mục" : "Thêm danh mục");

        TextField nameField = new TextField(isEdit ? existing.getName() : "");
        TextArea descField = new TextArea(isEdit ? existing.getDescription() : "");
        descField.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Tên danh mục:"), nameField);
        grid.addRow(1, new Label("Mô tả:"), descField);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String name = nameField.getText().trim();
                String desc = descField.getText();
                if (name.isEmpty()) {
                    DialogUtil.showError("Lỗi", "Tên danh mục không được để trống");
                    return null;
                }
                if (isEdit) {
                    AsyncTask.runVoid(
                            () -> apiService.updateCategory(existing.getId(), name, desc),
                            () -> { DialogUtil.showInfo("Thành công", "Cập nhật danh mục thành công"); loadData(); },
                            DialogUtil::showError);
                } else {
                    AsyncTask.runVoid(
                            () -> apiService.createCategory(name, desc),
                            () -> { DialogUtil.showInfo("Thành công", "Tạo danh mục thành công"); loadData(); },
                            DialogUtil::showError);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleDelete(CategoryDTO category) {
        if (!DialogUtil.confirm("Xác nhận xoá", "Xoá danh mục '" + category.getName() + "'?")) {
            return;
        }
        AsyncTask.runVoid(
                () -> apiService.deleteCategory(category.getId()),
                () -> { DialogUtil.showInfo("Thành công", "Đã xoá danh mục"); loadData(); },
                DialogUtil::showError);
    }
}
