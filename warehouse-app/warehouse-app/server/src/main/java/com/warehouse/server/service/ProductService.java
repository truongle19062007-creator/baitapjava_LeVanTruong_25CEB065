package com.warehouse.server.service;

import com.warehouse.server.dao.InventoryDAO;
import com.warehouse.server.dao.ProductDAO;
import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;
    private final InventoryDAO inventoryDAO;

    public ProductService(ProductDAO productDAO, InventoryDAO inventoryDAO) {
        this.productDAO = productDAO;
        this.inventoryDAO = inventoryDAO;
    }

    public List<Product> listAll() {
        try {
            return productDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách sản phẩm", e);
        }
    }

    public List<Product> search(String keyword) {
        try {
            if (keyword == null || keyword.isBlank()) {
                return productDAO.findAll();
            }
            return productDAO.search(keyword.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm sản phẩm", e);
        }
    }

    public Product get(Long id) {
        try {
            return productDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm", "NOT_FOUND"));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn sản phẩm", e);
        }
    }

    /**
     * Tạo sản phẩm mới + dòng tồn kho ban đầu (quantity=0) trong CÙNG 1 transaction.
     * Nếu insert inventory thất bại, rollback luôn cả việc tạo sản phẩm,
     * tránh tình trạng có sản phẩm nhưng không có dòng tồn kho tương ứng.
     */
    public Product create(String code, String name, Long categoryId, String unit,
                           BigDecimal importPrice, BigDecimal sellPrice, int minStock, String description) {
        validate(code, name, importPrice, sellPrice, minStock);
        try {
            if (productDAO.findByCode(code.trim()).isPresent()) {
                throw new BusinessException("Mã sản phẩm đã tồn tại", "CONFLICT");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra mã sản phẩm", e);
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Product p = new Product();
            p.setCode(code.trim());
            p.setName(name.trim());
            p.setCategoryId(categoryId);
            p.setUnit(unit == null || unit.isBlank() ? "cái" : unit.trim());
            p.setImportPrice(importPrice);
            p.setSellPrice(sellPrice);
            p.setMinStock(minStock);
            p.setDescription(description);

            productDAO.insert(conn, p);
            inventoryDAO.createInitialRow(conn, p.getId());

            conn.commit();
            return p;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi tạo sản phẩm", e);
        } finally {
            closeQuietly(conn);
        }
    }

    public void update(Long id, String name, Long categoryId, String unit,
                        BigDecimal importPrice, BigDecimal sellPrice, int minStock, String description) {
        validate("DUMMY-NOT-CHECKED", name, importPrice, sellPrice, minStock); // code không đổi khi update
        try {
            Product p = productDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm", "NOT_FOUND"));
            p.setName(name.trim());
            p.setCategoryId(categoryId);
            p.setUnit(unit == null || unit.isBlank() ? "cái" : unit.trim());
            p.setImportPrice(importPrice);
            p.setSellPrice(sellPrice);
            p.setMinStock(minStock);
            p.setDescription(description);
            productDAO.update(p);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật sản phẩm", e);
        }
    }

    public void delete(Long id) {
        try {
            productDAO.delete(id);
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new BusinessException(
                        "Không thể xoá sản phẩm đã có giao dịch nhập/xuất kho", "CONFLICT");
            }
            throw new RuntimeException("Lỗi xoá sản phẩm", e);
        }
    }

    private void validate(String code, String name, BigDecimal importPrice, BigDecimal sellPrice, int minStock) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Mã sản phẩm không được để trống", "VALIDATION_ERROR");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên sản phẩm không được để trống", "VALIDATION_ERROR");
        }
        if (importPrice == null || importPrice.signum() < 0) {
            throw new BusinessException("Giá nhập không hợp lệ", "VALIDATION_ERROR");
        }
        if (sellPrice == null || sellPrice.signum() < 0) {
            throw new BusinessException("Giá bán không hợp lệ", "VALIDATION_ERROR");
        }
        if (minStock < 0) {
            throw new BusinessException("Ngưỡng tồn kho tối thiểu không hợp lệ", "VALIDATION_ERROR");
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
