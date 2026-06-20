package com.warehouse.server.service;

import com.warehouse.server.dao.CategoryDAO;
import com.warehouse.server.model.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public List<Category> listAll() {
        try {
            return categoryDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh mục", e);
        }
    }

    public Category create(String name, String description) {
        validateName(name);
        try {
            Category c = new Category();
            c.setName(name.trim());
            c.setDescription(description);
            return categoryDAO.insert(c);
        } catch (SQLException e) {
            if (isDuplicateKeyError(e)) {
                throw new BusinessException("Tên danh mục đã tồn tại", "CONFLICT");
            }
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi tạo danh mục", e);
        }
    }

    public void update(Long id, String name, String description) {
        validateName(name);
        try {
            Category c = categoryDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy danh mục", "NOT_FOUND"));
            c.setName(name.trim());
            c.setDescription(description);
            categoryDAO.update(c);
        } catch (SQLException e) {
            if (isDuplicateKeyError(e)) {
                throw new BusinessException("Tên danh mục đã tồn tại", "CONFLICT");
            }
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi cập nhật danh mục", e);
        }
    }

    public void delete(Long id) {
        try {
            categoryDAO.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi xoá danh mục", e);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên danh mục không được để trống", "VALIDATION_ERROR");
        }
    }

    private boolean isDuplicateKeyError(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
