package com.warehouse.server.service;

import com.warehouse.server.dao.SupplierDAO;
import com.warehouse.server.model.Supplier;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierService(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    public List<Supplier> listAll() {
        try {
            return supplierDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn nhà cung cấp", e);
        }
    }

    public Supplier create(String name, String phone, String email, String address) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên nhà cung cấp không được để trống", "VALIDATION_ERROR");
        }
        try {
            Supplier s = new Supplier();
            s.setName(name.trim());
            s.setPhone(phone);
            s.setEmail(email);
            s.setAddress(address);
            return supplierDAO.insert(s);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi tạo nhà cung cấp", e);
        }
    }

    public void update(Long id, String name, String phone, String email, String address) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên nhà cung cấp không được để trống", "VALIDATION_ERROR");
        }
        try {
            Supplier s = supplierDAO.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy nhà cung cấp", "NOT_FOUND"));
            s.setName(name.trim());
            s.setPhone(phone);
            s.setEmail(email);
            s.setAddress(address);
            supplierDAO.update(s);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi cập nhật nhà cung cấp", e);
        }
    }

    public void delete(Long id) {
        try {
            supplierDAO.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn dữ liệu khi xoá nhà cung cấp", e);
        }
    }
}
