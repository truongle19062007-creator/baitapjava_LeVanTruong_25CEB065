package com.warehouse.shared.dto;

/** DTO dùng chung cho các request chỉ cần truyền 1 id (GET, DELETE...). */
public class IdRequestDTO {
    private Long id;

    public IdRequestDTO() {
    }

    public IdRequestDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
