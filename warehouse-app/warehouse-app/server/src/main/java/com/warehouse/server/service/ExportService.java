package com.warehouse.server.service;

import com.warehouse.server.dao.ExportReceiptDAO;
import com.warehouse.server.dao.InventoryDAO;
import com.warehouse.server.dao.ProductDAO;
import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.ExportReceipt;
import com.warehouse.server.model.Product;
import com.warehouse.server.model.ReceiptItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ExportService {

    private final ExportReceiptDAO exportReceiptDAO;
    private final InventoryDAO inventoryDAO;
    private final ProductDAO productDAO;

    public ExportService(ExportReceiptDAO exportReceiptDAO, InventoryDAO inventoryDAO, ProductDAO productDAO) {
        this.exportReceiptDAO = exportReceiptDAO;
        this.inventoryDAO = inventoryDAO;
        this.productDAO = productDAO;
    }

    public List<ExportReceipt> listAll() {
        try {
            return exportReceiptDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách phiếu xuất", e);
        }
    }

    public ExportReceipt get(Long id) {
        try {
            return exportReceiptDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu xuất", "NOT_FOUND"));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn phiếu xuất", e);
        }
    }

    /**
     * Tạo phiếu xuất kho. Với MỖI dòng sản phẩm:
     *  1. Lock dòng tồn kho (SELECT ... FOR UPDATE) để tránh 2 giao dịch xuất cùng lúc
     *     đọc cùng 1 số lượng tồn rồi cùng trừ, dẫn đến tồn kho âm (race condition).
     *  2. Kiểm tra tồn kho hiện tại >= số lượng cần xuất, nếu không đủ thì BusinessException.
     *  3. Trừ tồn kho.
     * Toàn bộ trong 1 transaction - nếu 1 dòng không đủ hàng, rollback toàn bộ phiếu.
     */
    public ExportReceipt createReceipt(String customerName, Long createdByUserId, String note, List<ReceiptItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Phiếu xuất phải có ít nhất 1 sản phẩm", "VALIDATION_ERROR");
        }
        for (ReceiptItem item : items) {
            if (item.getProductId() == null) {
                throw new BusinessException("Thiếu thông tin sản phẩm trong phiếu xuất", "VALIDATION_ERROR");
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng xuất phải lớn hơn 0", "VALIDATION_ERROR");
            }
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            BigDecimal total = BigDecimal.ZERO;

            for (ReceiptItem item : items) {
                Product product = productDAO.findById(item.getProductId())
                        .orElseThrow(() -> new BusinessException(
                                "Sản phẩm id=" + item.getProductId() + " không tồn tại", "NOT_FOUND"));

                // Nếu client không gửi giá xuất, lấy giá bán hiện tại của sản phẩm làm mặc định
                if (item.getPrice() == null) {
                    item.setPrice(product.getSellPrice());
                }
                if (item.getPrice().signum() < 0) {
                    throw new BusinessException("Giá xuất không hợp lệ", "VALIDATION_ERROR");
                }

                int currentQty = inventoryDAO.lockForUpdate(conn, item.getProductId());
                if (currentQty < item.getQuantity()) {
                    throw new BusinessException(
                            "Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn " + currentQty +
                                    ", cần xuất " + item.getQuantity() + ")", "INSUFFICIENT_STOCK");
                }

                total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            ExportReceipt receipt = new ExportReceipt();
            receipt.setCode(exportReceiptDAO.generateNextCode());
            receipt.setCustomerName(customerName);
            receipt.setCreatedByUserId(createdByUserId);
            receipt.setNote(note);
            receipt.setTotalAmount(total);

            exportReceiptDAO.insertHeader(conn, receipt);

            for (ReceiptItem item : items) {
                exportReceiptDAO.insertItem(conn, receipt.getId(), item);
                inventoryDAO.decrease(conn, item.getProductId(), item.getQuantity());
            }

            conn.commit();
            receipt.setItems(items);
            return receipt;
        } catch (BusinessException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi tạo phiếu xuất kho", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /** Xoá phiếu xuất: hoàn lại tồn kho đã trừ trước đó, trong cùng transaction. */
    public void deleteReceipt(Long id) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            ExportReceipt receipt = exportReceiptDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu xuất", "NOT_FOUND"));

            for (ReceiptItem item : receipt.getItems()) {
                inventoryDAO.lockForUpdate(conn, item.getProductId());
                inventoryDAO.increase(conn, item.getProductId(), item.getQuantity());
            }

            exportReceiptDAO.delete(id);
            conn.commit();
        } catch (BusinessException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi xoá phiếu xuất kho", e);
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
