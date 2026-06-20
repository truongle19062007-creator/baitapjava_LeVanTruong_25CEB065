package com.warehouse.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExportReceiptDTO {
    private Long id;
    private String code;             // mã phiếu xuất, VD: PX0001
    private String customerName;     // tên khách hàng/đơn vị nhận hàng (free text, không cần bảng riêng)
    private Long createdByUserId;
    private String createdByName;    // chỉ hiển thị
    private LocalDateTime createdAt;
    private String note;
    private BigDecimal totalAmount;  // server tính
    private List<ReceiptItemDTO> items;

    public ExportReceiptDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<ReceiptItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItemDTO> items) {
        this.items = items;
    }
}
