package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.CategoryDTO;
import com.warehouse.shared.dto.ProductDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;

public class ProductView {

    private final ApiService apiService;
    private final TableView<ProductDTO> table = new TableView<>();
    private final ObservableList<ProductDTO> data = FXCollections.observableArrayList();
    private TextField searchField;

    public ProductView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Quản lý Sản phẩm");
        title.getStyleClass().add("section-title");

        searchField = new TextField();
        searchField.setPromptText("Tìm theo mã hoặc tên sản phẩm...");
        searchField.setPrefWidth(280);
        searchField.setOnAction(e -> handleSearch());

        Button searchBtn = new Button("Tìm");
        searchBtn.getStyleClass().add("secondary-button");
        searchBtn.setOnAction(e -> handleSearch());

        Button addBtn = new Button("+ Thêm sản phẩm");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showFormDialog(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, searchField, searchBtn, spacer, addBtn);
        setupTable();

        VBox box = new VBox(15, title, toolbar, table);
        loadData();
        return box;
    }

    private void setupTable() {
        TableColumn<ProductDTO, String> codeCol = new TableColumn<>("Mã SP");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(90);

        TableColumn<ProductDTO, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<ProductDTO, String> catCol = new TableColumn<>("Danh mục");
        catCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        catCol.setPrefWidth(130);

        TableColumn<ProductDTO, String> unitCol = new TableColumn<>("ĐVT");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(70);

        TableColumn<ProductDTO, BigDecimal> importPriceCol = new TableColumn<>("Giá nhập");
        importPriceCol.setCellValueFactory(new PropertyValueFactory<>("importPrice"));
        importPriceCol.setPrefWidth(110);

        TableColumn<ProductDTO, BigDecimal> sellPriceCol = new TableColumn<>("Giá bán");
        sellPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        sellPriceCol.setPrefWidth(110);

        TableColumn<ProductDTO, Integer> minStockCol = new TableColumn<>("Tồn tối thiểu");
        minStockCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        minStockCol.setPrefWidth(100);

        TableColumn<ProductDTO, Void> actionCol = new TableColumn<>("Hành động");
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

        table.getColumns().addAll(codeCol, nameCol, catCol, unitCol, importPriceCol, sellPriceCol, minStockCol, actionCol);
        table.setItems(data);
        table.setPrefHeight(500);
    }

    private void loadData() {
        AsyncTask.run(apiService::listProducts, list -> data.setAll(list), DialogUtil::showError);
    }

    private void handleSearch() {
        String keyword = searchField.getText();
        AsyncTask.run(() -> apiService.searchProducts(keyword), list -> data.setAll(list), DialogUtil::showError);
    }

    private void showFormDialog(ProductDTO existing) {
        boolean isEdit = existing != null;

        AsyncTask.run(apiService::listCategories,
                (List<CategoryDTO> categories) -> renderFormDialog(existing, isEdit, categories),
                DialogUtil::showError);
    }

    private void renderFormDialog(ProductDTO existing, boolean isEdit, List<CategoryDTO> categories) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm");

        TextField codeField = new TextField(isEdit ? existing.getCode() : "");
        codeField.setDisable(isEdit); // Mã sản phẩm không cho sửa sau khi tạo
        TextField nameField = new TextField(isEdit ? existing.getName() : "");

        ComboBox<CategoryDTO> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(categories));
        categoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategoryDTO c) { return c == null ? "" : c.getName(); }
            @Override
            public CategoryDTO fromString(String s) { return null; }
        });
        if (isEdit && existing.getCategoryId() != null) {
            categories.stream().filter(c -> c.getId().equals(existing.getCategoryId()))
                    .findFirst().ifPresent(categoryCombo::setValue);
        }

        TextField unitField = new TextField(isEdit ? existing.getUnit() : "cái");
        TextField importPriceField = new TextField(isEdit ? existing.getImportPrice().toPlainString() : "0");
        TextField sellPriceField = new TextField(isEdit ? existing.getSellPrice().toPlainString() : "0");
        TextField minStockField = new TextField(isEdit ? String.valueOf(existing.getMinStock()) : "0");
        TextArea descField = new TextArea(isEdit ? existing.getDescription() : "");
        descField.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        int r = 0;
        grid.addRow(r++, new Label("Mã SP:"), codeField);
        grid.addRow(r++, new Label("Tên sản phẩm:"), nameField);
        grid.addRow(r++, new Label("Danh mục:"), categoryCombo);
        grid.addRow(r++, new Label("Đơn vị tính:"), unitField);
        grid.addRow(r++, new Label("Giá nhập:"), importPriceField);
        grid.addRow(r++, new Label("Giá bán:"), sellPriceField);
        grid.addRow(r++, new Label("Tồn tối thiểu:"), minStockField);
        grid.addRow(r++, new Label("Mô tả:"), descField);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                try {
                    ProductDTO dto = new ProductDTO();
                    dto.setId(isEdit ? existing.getId() : null);
                    dto.setCode(codeField.getText().trim());
                    dto.setName(nameField.getText().trim());
                    dto.setCategoryId(categoryCombo.getValue() == null ? null : categoryCombo.getValue().getId());
                    dto.setUnit(unitField.getText().trim());
                    dto.setImportPrice(new BigDecimal(importPriceField.getText().trim()));
                    dto.setSellPrice(new BigDecimal(sellPriceField.getText().trim()));
                    dto.setMinStock(Integer.parseInt(minStockField.getText().trim()));
                    dto.setDescription(descField.getText());

                    if (isEdit) {
                        AsyncTask.runVoid(
                                () -> apiService.updateProduct(dto),
                                () -> { DialogUtil.showInfo("Thành công", "Cập nhật sản phẩm thành công"); loadData(); },
                                DialogUtil::showError);
                    } else {
                        AsyncTask.runVoid(
                                () -> apiService.createProduct(dto),
                                () -> { DialogUtil.showInfo("Thành công", "Tạo sản phẩm thành công"); loadData(); },
                                DialogUtil::showError);
                    }
                } catch (NumberFormatException ex) {
                    DialogUtil.showError("Lỗi", "Giá hoặc số lượng nhập không hợp lệ (phải là số)");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleDelete(ProductDTO product) {
        if (!DialogUtil.confirm("Xác nhận xoá", "Xoá sản phẩm '" + product.getName() + "'?")) {
            return;
        }
        AsyncTask.runVoid(
                () -> apiService.deleteProduct(product.getId()),
                () -> { DialogUtil.showInfo("Thành công", "Đã xoá sản phẩm"); loadData(); },
                DialogUtil::showError);
    }
}
