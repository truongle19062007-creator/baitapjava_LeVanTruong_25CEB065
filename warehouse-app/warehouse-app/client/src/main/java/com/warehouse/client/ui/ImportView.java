package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ImportView {

    private final ApiService apiService;
    private final TableView<ImportReceiptDTO> table = new TableView<>();
    private final ObservableList<ImportReceiptDTO> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ImportView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Phiếu Nhập kho");
        title.getStyleClass().add("section-title");

        Button addBtn = new Button("+ Tạo phiếu nhập");
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
        TableColumn<ImportReceiptDTO, String> codeCol = new TableColumn<>("Mã phiếu");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(100);

        TableColumn<ImportReceiptDTO, String> supplierCol = new TableColumn<>("Nhà cung cấp");
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        supplierCol.setPrefWidth(180);

        TableColumn<ImportReceiptDTO, String> createdByCol = new TableColumn<>("Người tạo");
        createdByCol.setCellValueFactory(new PropertyValueFactory<>("createdByName"));
        createdByCol.setPrefWidth(140);

        TableColumn<ImportReceiptDTO, String> dateCol = new TableColumn<>("Ngày tạo");
        dateCol.setPrefWidth(140);
        dateCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getCreatedAt() == null ? "" : cd.getValue().getCreatedAt().format(DATE_FMT)));

        TableColumn<ImportReceiptDTO, BigDecimal> totalCol = new TableColumn<>("Tổng tiền");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalCol.setPrefWidth(140);

        TableColumn<ImportReceiptDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setPrefWidth(160);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Xem");
            private final Button deleteBtn = new Button("Xoá");
            {
                viewBtn.getStyleClass().add("secondary-button");
                deleteBtn.getStyleClass().add("danger-button");
                viewBtn.setOnAction(e -> showDetailDialog(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(8, viewBtn, deleteBtn));
            }
        });

        table.getColumns().addAll(codeCol, supplierCol, createdByCol, dateCol, totalCol, actionCol);
        table.setItems(data);
        table.setPrefHeight(500);
    }

    private void loadData() {
        AsyncTask.run(apiService::listImportReceipts, list -> data.setAll(list), DialogUtil::showError);
    }

    private void showDetailDialog(ImportReceiptDTO row) {
        AsyncTask.run(() -> apiService.getImportReceipt(row.getId()), this::renderDetailDialog, DialogUtil::showError);
    }

    private void renderDetailDialog(ImportReceiptDTO receipt) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết phiếu nhập " + receipt.getCode());

        TableView<ReceiptItemDTO> itemTable = new TableView<>();
        TableColumn<ReceiptItemDTO, String> codeCol = new TableColumn<>("Mã SP");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        TableColumn<ReceiptItemDTO, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(200);
        TableColumn<ReceiptItemDTO, Integer> qtyCol = new TableColumn<>("SL");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<ReceiptItemDTO, BigDecimal> priceCol = new TableColumn<>("Giá nhập");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<ReceiptItemDTO, BigDecimal> subtotalCol = new TableColumn<>("Thành tiền");
        subtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        itemTable.getColumns().addAll(codeCol, nameCol, qtyCol, priceCol, subtotalCol);
        itemTable.setItems(FXCollections.observableArrayList(receipt.getItems()));
        itemTable.setPrefHeight(250);
        itemTable.setPrefWidth(600);

        Label info = new Label("Nhà cung cấp: " + (receipt.getSupplierName() == null ? "(không có)" : receipt.getSupplierName()) +
                "\nNgười tạo: " + receipt.getCreatedByName() +
                "\nGhi chú: " + (receipt.getNote() == null ? "" : receipt.getNote()) +
                "\nTổng tiền: " + receipt.getTotalAmount());

        VBox content = new VBox(10, info, itemTable);
        content.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleDelete(ImportReceiptDTO receipt) {
        if (!DialogUtil.confirm("Xác nhận xoá", "Xoá phiếu nhập '" + receipt.getCode() +
                "'? Tồn kho sẽ bị trừ lại tương ứng.")) {
            return;
        }
        AsyncTask.runVoid(
                () -> apiService.deleteImportReceipt(receipt.getId()),
                () -> { DialogUtil.showInfo("Thành công", "Đã xoá phiếu nhập"); loadData(); },
                DialogUtil::showError);
    }

    // ============ TẠO PHIẾU NHẬP MỚI ============

    private void showCreateDialog() {
        AsyncTask.run(
                () -> {
                    List<SupplierDTO> suppliers = apiService.listSuppliers();
                    List<ProductDTO> products = apiService.listProducts();
                    return new Object[]{suppliers, products};
                },
                (Object[] arr) -> renderCreateDialog((List<SupplierDTO>) arr[0], (List<ProductDTO>) arr[1]),
                DialogUtil::showError
        );
    }

    @SuppressWarnings("unchecked")
    private void renderCreateDialog(List<SupplierDTO> suppliers, List<ProductDTO> products) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tạo phiếu nhập kho");
        dialog.setResizable(true);

        ComboBox<SupplierDTO> supplierCombo = new ComboBox<>(FXCollections.observableArrayList(suppliers));
        supplierCombo.setConverter(nameConverter(SupplierDTO::getName));
        supplierCombo.setPromptText("Chọn nhà cung cấp (tuỳ chọn)");

        TextArea noteField = new TextArea();
        noteField.setPrefRowCount(2);
        noteField.setPromptText("Ghi chú (tuỳ chọn)");

        ObservableList<ImportLineRow> lines = FXCollections.observableArrayList();
        TableView<ImportLineRow> lineTable = buildLineTable(lines, products);

        Button addLineBtn = new Button("+ Thêm dòng sản phẩm");
        addLineBtn.getStyleClass().add("secondary-button");
        addLineBtn.setOnAction(e -> {
            if (!products.isEmpty()) {
                lines.add(new ImportLineRow(products.get(0), 1, products.get(0).getImportPrice()));
            }
        });

        VBox content = new VBox(10,
                new Label("Nhà cung cấp:"), supplierCombo,
                new Label("Ghi chú:"), noteField,
                new Separator(),
                addLineBtn, lineTable);
        content.setPadding(new Insets(15));
        content.setPrefWidth(650);

        dialog.getDialogPane().setContent(content);
        ButtonType saveType = new ButtonType("Lưu phiếu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                if (lines.isEmpty()) {
                    DialogUtil.showError("Lỗi", "Phiếu nhập phải có ít nhất 1 dòng sản phẩm");
                    return null;
                }
                List<ReceiptItemDTO> items = new ArrayList<>();
                for (ImportLineRow row : lines) {
                    if (row.getQuantity() <= 0) {
                        DialogUtil.showError("Lỗi", "Số lượng phải lớn hơn 0");
                        return null;
                    }
                    ReceiptItemDTO item = new ReceiptItemDTO();
                    item.setProductId(row.getProduct().getId());
                    item.setQuantity(row.getQuantity());
                    item.setPrice(row.getPrice());
                    items.add(item);
                }
                ImportReceiptDTO dto = new ImportReceiptDTO();
                dto.setSupplierId(supplierCombo.getValue() == null ? null : supplierCombo.getValue().getId());
                dto.setNote(noteField.getText());
                dto.setItems(items);

                AsyncTask.runVoid(
                        () -> apiService.createImportReceipt(dto),
                        () -> { DialogUtil.showInfo("Thành công", "Tạo phiếu nhập kho thành công"); loadData(); },
                        DialogUtil::showError);
            }
            return null;
        });

        dialog.showAndWait();
    }

    private TableView<ImportLineRow> buildLineTable(ObservableList<ImportLineRow> lines, List<ProductDTO> products) {
        TableView<ImportLineRow> tv = new TableView<>(lines);
        tv.setEditable(true);
        tv.setPrefHeight(220);

        TableColumn<ImportLineRow, ProductDTO> productCol = new TableColumn<>("Sản phẩm");
        productCol.setPrefWidth(220);
        productCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getProduct()));
        productCol.setCellFactory(col -> new javafx.scene.control.cell.ComboBoxTableCell<>(
                nameConverter(ProductDTO::getName), FXCollections.observableArrayList(products)));
        productCol.setOnEditCommit(e -> {
            ImportLineRow row = e.getRowValue();
            row.setProduct(e.getNewValue());
            row.setPrice(e.getNewValue().getImportPrice());
            tv.refresh();
        });

        TableColumn<ImportLineRow, Integer> qtyCol = new TableColumn<>("Số lượng");
        qtyCol.setPrefWidth(100);
        qtyCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getQuantity()).asObject());
        qtyCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        qtyCol.setOnEditCommit(e -> e.getRowValue().setQuantity(e.getNewValue()));

        TableColumn<ImportLineRow, BigDecimal> priceCol = new TableColumn<>("Giá nhập");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getPrice()));
        priceCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        priceCol.setOnEditCommit(e -> e.getRowValue().setPrice(e.getNewValue()));

        TableColumn<ImportLineRow, Void> removeCol = new TableColumn<>("");
        removeCol.setPrefWidth(70);
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Xoá");
            {
                removeBtn.getStyleClass().add("danger-button");
                removeBtn.setOnAction(e -> lines.remove(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        tv.getColumns().addAll(productCol, qtyCol, priceCol, removeCol);
        return tv;
    }

    private <T> StringConverter<T> nameConverter(java.util.function.Function<T, String> nameFn) {
        return new StringConverter<>() {
            @Override
            public String toString(T obj) { return obj == null ? "" : nameFn.apply(obj); }
            @Override
            public T fromString(String s) { return null; }
        };
    }

    /** Model dòng tạm dùng trong dialog tạo phiếu nhập (chưa gửi server). */
    public static class ImportLineRow {
        private ProductDTO product;
        private int quantity;
        private BigDecimal price;

        public ImportLineRow(ProductDTO product, int quantity, BigDecimal price) {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
        }

        public ProductDTO getProduct() { return product; }
        public void setProduct(ProductDTO product) { this.product = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    private static class IntegerStringConverter extends javafx.util.StringConverter<Integer> {
        @Override public String toString(Integer object) { return object == null ? "0" : object.toString(); }
        @Override public Integer fromString(String string) {
            try { return Integer.parseInt(string.trim()); } catch (Exception e) { return 0; }
        }
    }

    private static class BigDecimalStringConverter extends javafx.util.StringConverter<BigDecimal> {
        @Override public String toString(BigDecimal object) { return object == null ? "0" : object.toPlainString(); }
        @Override public BigDecimal fromString(String string) {
            try { return new BigDecimal(string.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
        }
    }
}
