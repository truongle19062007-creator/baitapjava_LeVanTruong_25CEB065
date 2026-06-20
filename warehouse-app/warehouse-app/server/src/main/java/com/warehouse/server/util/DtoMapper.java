package com.warehouse.server.util;

import com.warehouse.server.model.*;
import com.warehouse.shared.dto.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chuyển đổi Model (entity nội bộ server, có thể chứa field nhạy cảm như passwordHash)
 * sang DTO (object gửi qua mạng cho client). Tách riêng để đảm bảo KHÔNG BAO GIỜ
 * vô tình serialize field nhạy cảm (passwordHash) ra ngoài.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserDTO toDto(User u) {
        if (u == null) return null;
        return new UserDTO(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isActive(), u.getCreatedAt());
    }

    public static List<UserDTO> toUserDtoList(List<User> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static CategoryDTO toDto(Category c) {
        if (c == null) return null;
        return new CategoryDTO(c.getId(), c.getName(), c.getDescription());
    }

    public static List<CategoryDTO> toCategoryDtoList(List<Category> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static SupplierDTO toDto(Supplier s) {
        if (s == null) return null;
        return new SupplierDTO(s.getId(), s.getName(), s.getPhone(), s.getEmail(), s.getAddress());
    }

    public static List<SupplierDTO> toSupplierDtoList(List<Supplier> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static ProductDTO toDto(Product p) {
        if (p == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setCategoryId(p.getCategoryId());
        dto.setCategoryName(p.getCategoryName());
        dto.setUnit(p.getUnit());
        dto.setImportPrice(p.getImportPrice());
        dto.setSellPrice(p.getSellPrice());
        dto.setMinStock(p.getMinStock());
        dto.setDescription(p.getDescription());
        return dto;
    }

    public static List<ProductDTO> toProductDtoList(List<Product> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static InventoryDTO toDto(Inventory inv) {
        if (inv == null) return null;
        InventoryDTO dto = new InventoryDTO();
        dto.setProductId(inv.getProductId());
        dto.setProductCode(inv.getProductCode());
        dto.setProductName(inv.getProductName());
        dto.setUnit(inv.getUnit());
        dto.setQuantity(inv.getQuantity());
        dto.setMinStock(inv.getMinStock());
        dto.setLowStock(inv.getQuantity() <= inv.getMinStock());
        return dto;
    }

    public static List<InventoryDTO> toInventoryDtoList(List<Inventory> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static ReceiptItemDTO toDto(ReceiptItem item) {
        if (item == null) return null;
        ReceiptItemDTO dto = new ReceiptItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductCode(item.getProductCode());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    public static List<ReceiptItemDTO> toItemDtoList(List<ReceiptItem> items) {
        return items.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static ImportReceiptDTO toDto(ImportReceipt r) {
        if (r == null) return null;
        ImportReceiptDTO dto = new ImportReceiptDTO();
        dto.setId(r.getId());
        dto.setCode(r.getCode());
        dto.setSupplierId(r.getSupplierId());
        dto.setSupplierName(r.getSupplierName());
        dto.setCreatedByUserId(r.getCreatedByUserId());
        dto.setCreatedByName(r.getCreatedByName());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setNote(r.getNote());
        dto.setTotalAmount(r.getTotalAmount());
        if (r.getItems() != null) {
            dto.setItems(toItemDtoList(r.getItems()));
        }
        return dto;
    }

    public static List<ImportReceiptDTO> toImportDtoList(List<ImportReceipt> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static ExportReceiptDTO toDto(ExportReceipt r) {
        if (r == null) return null;
        ExportReceiptDTO dto = new ExportReceiptDTO();
        dto.setId(r.getId());
        dto.setCode(r.getCode());
        dto.setCustomerName(r.getCustomerName());
        dto.setCreatedByUserId(r.getCreatedByUserId());
        dto.setCreatedByName(r.getCreatedByName());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setNote(r.getNote());
        dto.setTotalAmount(r.getTotalAmount());
        if (r.getItems() != null) {
            dto.setItems(toItemDtoList(r.getItems()));
        }
        return dto;
    }

    public static List<ExportReceiptDTO> toExportDtoList(List<ExportReceipt> list) {
        return list.stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    public static ReceiptItem fromDto(ReceiptItemDTO dto) {
        ReceiptItem item = new ReceiptItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setPrice(dto.getPrice());
        return item;
    }

    public static List<ReceiptItem> fromItemDtoList(List<ReceiptItemDTO> list) {
        return list.stream().map(DtoMapper::fromDto).collect(Collectors.toList());
    }
}
