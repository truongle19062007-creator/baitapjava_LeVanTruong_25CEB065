package com.warehouse.client.ui;

import com.warehouse.client.service.ApiService;
import com.warehouse.client.util.AsyncTask;
import com.warehouse.client.util.DialogUtil;
import com.warehouse.shared.dto.InventoryDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class InventoryView {

    private final ApiService apiService;
    private final TableView<InventoryDTO> table = new TableView<>();
    private final ObservableList<InventoryDTO> data = FXCollections.observableArrayList();
    private CheckBox lowStockOnlyCheck;

    public InventoryView(ApiService apiService) {
        this.apiService = apiService;
    }

    public Parent getView() {
        Label title = new Label("Tồn kho hiện tại");
        title.getStyleClass().add("section-title");

        lowStockOnlyCheck = new CheckBox("Chỉ hiển thị tồn kho thấp");
        lowStockOnlyCheck.setOnAction(e -> loadData());

        Button refreshBtn = new Button("Tải lại");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> loadData());

        HBox toolbar = new HBox(15, lowStockOnlyCheck, refreshBtn);
        setupTable();

        VBox box = new VBox(15, title, toolbar, table);
        loadData();
        return box;
    }

    private void setupTable() {
        TableColumn<InventoryDTO, String> codeCol = new TableColumn<>("Mã SP");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        codeCol.setPrefWidth(100);

        TableColumn<InventoryDTO, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(280);

        TableColumn<InventoryDTO, String> unitCol = new TableColumn<>("ĐVT");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(80);

        TableColumn<InventoryDTO, Integer> qtyCol = new TableColumn<>("Số lượng tồn");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(120);

        TableColumn<InventoryDTO, Integer> minCol = new TableColumn<>("Tồn tối thiểu");
        minCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        minCol.setPrefWidth(120);

        TableColumn<InventoryDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setPrefWidth(150);
        statusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().isLowStock() ? "⚠ Tồn kho thấp" : "Bình thường"));

        table.getColumns().addAll(codeCol, nameCol, unitCol, qtyCol, minCol, statusCol);
        table.setItems(data);
        table.setPrefHeight(550);

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(InventoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isLowStock()) {
                    setStyle("-fx-background-color: #ffebee;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadData() {
        if (lowStockOnlyCheck.isSelected()) {
            AsyncTask.run(apiService::listLowStock, list -> data.setAll(list), DialogUtil::showError);
        } else {
            AsyncTask.run(apiService::listInventory, list -> data.setAll(list), DialogUtil::showError);
        }
    }
}
