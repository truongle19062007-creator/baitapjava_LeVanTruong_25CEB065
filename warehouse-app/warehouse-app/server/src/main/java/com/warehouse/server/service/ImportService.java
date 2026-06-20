package com.warehouse.server.service;

import com.warehouse.server.dao.ImportReceiptDAO;
import com.warehouse.server.dao.InventoryDAO;
import com.warehouse.server.dao.ProductDAO;
import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.ImportReceipt;
import com.warehouse.server.model.ReceiptItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ImportService {

    private final ImportReceiptDAO importReceiptDAO;
    private final InventoryDAO inventoryDAO;
    private final ProductDAO productDAO;

    public ImportService(ImportReceiptDAO importReceiptDAO, InventoryDAO inventoryDAO, ProductDAO productDAO) {
        this.importReceiptDAO = importReceiptDAO;
        this.inventoryDAO = inventoryDAO;
        this.productDAO = productDAO;
    }

    public List<ImportReceipt> listAll() {
        try {
            return importReceiptDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách phiếu nhập", e);
        }
    }

    public ImportReceipt get(Long id) {
        try {
            return importReceiptDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu nhập", "NOT_FOUND"));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn phiếu nhập", e);
        }
    }

    /**
     * Tạo phiếu nhập kho: ghi header + từng dòng chi tiết + tăng tồn kho tương ứng.
     * Toàn bộ nằm trong 1 transaction: nếu bất kỳ bước nào lỗi (VD: sản phẩm không tồn tại),
     * rollback toàn bộ để tránh phiếu nhập "mồ côi" hoặc tồn kho bị lệch.
     */
    public ImportReceipt createReceipt(Long supplierId, Long createdByUserId, String note, List<ReceiptItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Phiếu nhập phải có ít nhất 1 sản phẩm", "VALIDATION_ERROR");
        }
        for (ReceiptItem item : items) {
            if (item.getProductId() == null) {
                throw new BusinessException("Thiếu thông tin sản phẩm trong phiếu nhập", "VALIDATION_ERROR");
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng nhập phải lớn hơn 0", "VALIDATION_ERROR");
            }
            if (item.getPrice() == null || item.getPrice().signum() < 0) {
                throw new BusinessException("Giá nhập không hợp lệ", "VALIDATION_ERROR");
            }
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Kiểm tra tất cả sản phẩm tồn tại trước khi ghi gì cả
            for (ReceiptItem item : items) {
                if (productDAO.findById(item.getProductId()).isEmpty()) {
                    throw new BusinessException("Sản phẩm id=" + item.getProductId() + " không tồn tại", "NOT_FOUND");
                }
            }

            BigDecimal total = BigDecimal.ZERO;
            for (ReceiptItem item : items) {
                total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            ImportReceipt receipt = new ImportReceipt();
            receipt.setCode(importReceiptDAO.generateNextCode());
            receipt.setSupplierId(supplierId);
            receipt.setCreatedByUserId(createdByUserId);
            receipt.setNote(note);
            receipt.setTotalAmount(total);

            importReceiptDAO.insertHeader(conn, receipt);

            for (ReceiptItem item : items) {
                importReceiptDAO.insertItem(conn, receipt.getId(), item);
                // Lock dòng tồn kho trước khi cộng, đảm bảo nhất quán khi có nhiều giao dịch đồng thời
                inventoryDAO.lockForUpdate(conn, item.getProductId());
                inventoryDAO.increase(conn, item.getProductId(), item.getQuantity());
            }

            conn.commit();
            receipt.setItems(items);
            return receipt;
        } catch (BusinessException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi tạo phiếu nhập kho", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Xoá phiếu nhập: phải trừ lại tồn kho đã cộng trước đó, trong cùng transaction.
     * Nếu tồn kho hiện tại không đủ để trừ lại (VD: hàng đã được xuất đi sau đó),
     * từ chối xoá để tránh tồn kho âm.
     */
    public void deleteReceipt(Long id) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            ImportReceipt receipt = importReceiptDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu nhập", "NOT_FOUND"));

            for (ReceiptItem item : receipt.getItems()) {
                int current = inventoryDAO.lockForUpdate(conn, item.getProductId());
                if (current < item.getQuantity()) {
                    throw new BusinessException(
                            "Không thể xoá phiếu: tồn kho sản phẩm '" + item.getProductName() +
                                    "' hiện không đủ để hoàn lại (có thể đã được xuất kho)", "CONFLICT");
                }
                inventoryDAO.decrease(conn, item.getProductId(), item.getQuantity());
            }

            importReceiptDAO.delete(id);
            conn.commit();
        } catch (BusinessException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi xoá phiếu nhập kho", e);
        } finally {
            closeQuietly(conn);
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
