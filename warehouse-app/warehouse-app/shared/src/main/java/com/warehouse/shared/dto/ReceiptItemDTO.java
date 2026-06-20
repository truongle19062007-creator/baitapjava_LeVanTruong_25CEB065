package com.warehouse.shared.dto;

import java.math.BigDecimal;

/**
 * Dùng chung cho cả chi tiết phiếu nhập (ImportReceiptDTO) và phiếu xuất (ExportReceiptDTO).
 * Mỗi dòng tương ứng 1 sản phẩm trong phiếu.
 */
public class ReceiptItemDTO {
    private Long id;
    private Long productId;
    private String productCode;  // chỉ để hiển thị
    private String productName;  // chỉ để hiển thị
    private int quantity;
    private BigDecimal price;    // giá nhập hoặc giá xuất tại thời điểm giao dịch
    private BigDecimal subtotal; // quantity * price (server tính, client không cần gửi)

    public ReceiptItemDTO() {
    }

    public ReceiptItemDTO(Long productId, int quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
