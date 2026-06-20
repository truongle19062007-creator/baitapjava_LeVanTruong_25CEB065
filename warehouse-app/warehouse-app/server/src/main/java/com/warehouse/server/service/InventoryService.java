package com.warehouse.server.service;

import com.warehouse.server.dao.InventoryDAO;
import com.warehouse.server.model.Inventory;

import java.sql.SQLException;
import java.util.List;

public class InventoryService {

    private final InventoryDAO inventoryDAO;

    public InventoryService(InventoryDAO inventoryDAO) {
        this.inventoryDAO = inventoryDAO;
    }

    public List<Inventory> listAll() {
        try {
            return inventoryDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn tồn kho", e);
        }
    }

    public List<Inventory> listLowStock() {
        try {
            return inventoryDAO.findLowStock();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn tồn kho thấp", e);
        }
    }
}
