package com.warehouse.server.dao;

import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id ";

    public List<Product> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY p.name";
        List<Product> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Product> search(String keyword) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.code LIKE ? OR p.name LIKE ? ORDER BY p.name";
        List<Product> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public Optional<Product> findById(Long id) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    public Optional<Product> findByCode(String code) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    /** Insert sản phẩm mới. Việc tạo dòng inventory tương ứng do ProductService quyết định (cùng 1 transaction). */
    public Product insert(Connection conn, Product p) throws SQLException {
        String sql = "INSERT INTO products (code, name, category_id, unit, import_price, sell_price, min_stock, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            setNullableLong(ps, 3, p.getCategoryId());
            ps.setString(4, p.getUnit());
            ps.setBigDecimal(5, p.getImportPrice());
            ps.setBigDecimal(6, p.getSellPrice());
            ps.setInt(7, p.getMinStock());
            ps.setString(8, p.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getLong(1));
            }
            return p;
        }
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE products SET name = ?, category_id = ?, unit = ?, import_price = ?, " +
                "sell_price = ?, min_stock = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            setNullableLong(ps, 2, p.getCategoryId());
            ps.setString(3, p.getUnit());
            ps.setBigDecimal(4, p.getImportPrice());
            ps.setBigDecimal(5, p.getSellPrice());
            ps.setInt(6, p.getMinStock());
            ps.setString(7, p.getDescription());
            ps.setLong(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        long catId = rs.getLong("category_id");
        p.setCategoryId(rs.wasNull() ? null : catId);
        p.setCategoryName(rs.getString("category_name"));
        p.setUnit(rs.getString("unit"));
        p.setImportPrice(rs.getBigDecimal("import_price"));
        p.setSellPrice(rs.getBigDecimal("sell_price"));
        p.setMinStock(rs.getInt("min_stock"));
        p.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}
