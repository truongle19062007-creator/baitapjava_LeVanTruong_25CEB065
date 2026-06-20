package com.warehouse.server.dao;

import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.Inventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho bảng inventory (tồn kho).
 *
 * Quan trọng: các method increase/decrease/lockForUpdate nhận Connection từ bên ngoài
 * (do ImportService/ExportService truyền vào) để đảm bảo nằm trong cùng 1 transaction
 * với việc ghi receipt - tránh tình trạng lưu phiếu nhập/xuất thành công nhưng tồn kho
 * không cập nhật (hoặc ngược lại) khi có lỗi giữa đường.
 */
public class InventoryDAO {

    public List<Inventory> findAll() throws SQLException {
        String sql = "SELECT i.product_id, p.code AS product_code, p.name AS product_name, " +
                "p.unit, i.quantity, p.min_stock " +
                "FROM inventory i JOIN products p ON i.product_id = p.id " +
                "ORDER BY p.name";
        List<Inventory> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Inventory> findLowStock() throws SQLException {
        String sql = "SELECT i.product_id, p.code AS product_code, p.name AS product_name, " +
                "p.unit, i.quantity, p.min_stock " +
                "FROM inventory i JOIN products p ON i.product_id = p.id " +
                "WHERE i.quantity <= p.min_stock ORDER BY p.name";
        List<Inventory> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Optional<Integer> getQuantity(Long productId) throws SQLException {
        String sql = "SELECT quantity FROM inventory WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getInt("quantity"));
                return Optional.empty();
            }
        }
    }

    /** Tạo dòng tồn kho ban đầu (quantity = 0) khi tạo sản phẩm mới. Dùng trong transaction của ProductService. */
    public void createInitialRow(Connection conn, Long productId) throws SQLException {
        String sql = "INSERT INTO inventory (product_id, quantity) VALUES (?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Lock dòng tồn kho của 1 sản phẩm để đọc số lượng hiện tại trước khi xuất kho.
     * Dùng SELECT ... FOR UPDATE để các thread/transaction khác phải đợi, tránh
     * 2 client cùng xuất kho 1 sản phẩm vượt quá số lượng tồn thực tế (lost update).
     * PHẢI gọi trong transaction (conn.setAutoCommit(false)) và commit/rollback ở service.
     */
    public int lockForUpdate(Connection conn, Long productId) throws SQLException {
        String sql = "SELECT quantity FROM inventory WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
                throw new SQLException("Không tìm thấy dòng tồn kho cho product_id=" + productId);
            }
        }
    }

    /** Cộng thêm số lượng (dùng khi nhập kho). Phải gọi sau lockForUpdate trong cùng transaction. */
    public void increase(Connection conn, Long productId, int amount) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Trừ số lượng (dùng khi xuất kho). Phải gọi sau lockForUpdate trong cùng transaction
     * để đảm bảo currentQty đã được lock và còn đủ hàng trước khi trừ.
     */
    public void decrease(Connection conn, Long productId, int amount) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    private Inventory mapRow(ResultSet rs) throws SQLException {
        Inventory inv = new Inventory();
        inv.setProductId(rs.getLong("product_id"));
        inv.setProductCode(rs.getString("product_code"));
        inv.setProductName(rs.getString("product_name"));
        inv.setUnit(rs.getString("unit"));
        inv.setQuantity(rs.getInt("quantity"));
        inv.setMinStock(rs.getInt("min_stock"));
        return inv;
    }
}
