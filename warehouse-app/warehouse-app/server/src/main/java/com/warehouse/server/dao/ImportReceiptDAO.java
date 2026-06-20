package com.warehouse.server.dao;

import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.ImportReceipt;
import com.warehouse.server.model.ReceiptItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportReceiptDAO {

    private static final String BASE_SELECT =
            "SELECT r.*, s.name AS supplier_name, u.full_name AS created_by_name " +
            "FROM import_receipts r " +
            "LEFT JOIN suppliers s ON r.supplier_id = s.id " +
            "JOIN users u ON r.created_by = u.id ";

    public List<ImportReceipt> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY r.created_at DESC";
        List<ImportReceipt> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Optional<ImportReceipt> findById(Long id) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                ImportReceipt receipt = mapRow(rs);
                receipt.setItems(findItems(conn, id));
                return Optional.of(receipt);
            }
        }
    }

    public List<ReceiptItem> findItems(Connection conn, Long receiptId) throws SQLException {
        String sql = "SELECT ri.*, p.code AS product_code, p.name AS product_name " +
                "FROM import_receipt_items ri JOIN products p ON ri.product_id = p.id " +
                "WHERE ri.import_receipt_id = ?";
        List<ReceiptItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReceiptItem item = new ReceiptItem();
                    item.setId(rs.getLong("id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setProductCode(rs.getString("product_code"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getBigDecimal("price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /** Insert phiếu nhập (header). Trả về receipt với id đã set. Phải gọi trong transaction. */
    public ImportReceipt insertHeader(Connection conn, ImportReceipt receipt) throws SQLException {
        String sql = "INSERT INTO import_receipts (code, supplier_id, created_by, note, total_amount) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, receipt.getCode());
            if (receipt.getSupplierId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, receipt.getSupplierId());
            }
            ps.setLong(3, receipt.getCreatedByUserId());
            ps.setString(4, receipt.getNote());
            ps.setBigDecimal(5, receipt.getTotalAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) receipt.setId(keys.getLong(1));
            }
            return receipt;
        }
    }

    /** Insert 1 dòng chi tiết phiếu nhập. Phải gọi trong cùng transaction với insertHeader. */
    public void insertItem(Connection conn, Long receiptId, ReceiptItem item) throws SQLException {
        String sql = "INSERT INTO import_receipt_items (import_receipt_id, product_id, quantity, price) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, receiptId);
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getPrice());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        // Lưu ý: xoá phiếu nhập KHÔNG tự động hoàn lại tồn kho.
        // ImportService phải tự xử lý việc trừ lại tồn kho trước khi gọi delete này (cùng transaction).
        String sql = "DELETE FROM import_receipts WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public String generateNextCode() throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM import_receipts";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = rs.next() ? rs.getInt("cnt") : 0;
            return String.format("PN%05d", count + 1);
        }
    }

    private ImportReceipt mapRow(ResultSet rs) throws SQLException {
        ImportReceipt r = new ImportReceipt();
        r.setId(rs.getLong("id"));
        r.setCode(rs.getString("code"));
        long supId = rs.getLong("supplier_id");
        r.setSupplierId(rs.wasNull() ? null : supId);
        r.setSupplierName(rs.getString("supplier_name"));
        r.setCreatedByUserId(rs.getLong("created_by"));
        r.setCreatedByName(rs.getString("created_by_name"));
        r.setNote(rs.getString("note"));
        r.setTotalAmount(rs.getBigDecimal("total_amount"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }
}
