package com.warehouse.shared.protocol;

/**
 * Hằng số tên action dùng trong Request.action.
 * Dùng chung giữa Client và Server để tránh lệch tên do gõ tay (typo).
 */
public final class Actions {

    private Actions() {
    }

    // ===== AUTH =====
    public static final String AUTH_LOGIN = "AUTH.LOGIN";
    public static final String AUTH_LOGOUT = "AUTH.LOGOUT";
    public static final String AUTH_CHANGE_PASSWORD = "AUTH.CHANGE_PASSWORD";

    // ===== USER (quản lý người dùng - chỉ ADMIN) =====
    public static final String USER_LIST = "USER.LIST";
    public static final String USER_CREATE = "USER.CREATE";
    public static final String USER_UPDATE = "USER.UPDATE";
    public static final String USER_DELETE = "USER.DELETE";
    public static final String USER_RESET_PASSWORD = "USER.RESET_PASSWORD";

    // ===== PRODUCT (sản phẩm) =====
    public static final String PRODUCT_LIST = "PRODUCT.LIST";
    public static final String PRODUCT_GET = "PRODUCT.GET";
    public static final String PRODUCT_CREATE = "PRODUCT.CREATE";
    public static final String PRODUCT_UPDATE = "PRODUCT.UPDATE";
    public static final String PRODUCT_DELETE = "PRODUCT.DELETE";
    public static final String PRODUCT_SEARCH = "PRODUCT.SEARCH";

    // ===== CATEGORY (danh mục sản phẩm) =====
    public static final String CATEGORY_LIST = "CATEGORY.LIST";
    public static final String CATEGORY_CREATE = "CATEGORY.CREATE";
    public static final String CATEGORY_UPDATE = "CATEGORY.UPDATE";
    public static final String CATEGORY_DELETE = "CATEGORY.DELETE";

    // ===== SUPPLIER (nhà cung cấp) =====
    public static final String SUPPLIER_LIST = "SUPPLIER.LIST";
    public static final String SUPPLIER_CREATE = "SUPPLIER.CREATE";
    public static final String SUPPLIER_UPDATE = "SUPPLIER.UPDATE";
    public static final String SUPPLIER_DELETE = "SUPPLIER.DELETE";

    // ===== IMPORT (nhập kho) =====
    public static final String IMPORT_LIST = "IMPORT.LIST";
    public static final String IMPORT_GET = "IMPORT.GET";
    public static final String IMPORT_CREATE = "IMPORT.CREATE";
    public static final String IMPORT_DELETE = "IMPORT.DELETE";

    // ===== EXPORT (xuất kho) =====
    public static final String EXPORT_LIST = "EXPORT.LIST";
    public static final String EXPORT_GET = "EXPORT.GET";
    public static final String EXPORT_CREATE = "EXPORT.CREATE";
    public static final String EXPORT_DELETE = "EXPORT.DELETE";

    // ===== INVENTORY (tồn kho) =====
    public static final String INVENTORY_LIST = "INVENTORY.LIST";
    public static final String INVENTORY_LOW_STOCK = "INVENTORY.LOW_STOCK";
    public static final String INVENTORY_HISTORY = "INVENTORY.HISTORY";
}
