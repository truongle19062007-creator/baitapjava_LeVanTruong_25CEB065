package com.warehouse.shared.dto;

public class KeywordRequestDTO {
    private String keyword;

    public KeywordRequestDTO() {
    }

    public KeywordRequestDTO(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
