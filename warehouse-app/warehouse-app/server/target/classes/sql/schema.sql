-- ============================================================
-- WAREHOUSE MANAGEMENT - DATABASE SCHEMA (MySQL 8)
-- Chạy file này trước khi start server lần đầu:
--   mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS warehouse_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE warehouse_db;

-- ============================================================
-- BẢNG NGƯỜI DÙNG
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,   -- BCrypt hash, KHÔNG lưu plaintext
    full_name       VARCHAR(150) NOT NULL,
    role            ENUM('ADMIN', 'MANAGER', 'STAFF') NOT NULL DEFAULT 'STAFF',
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG DANH MỤC SẢN PHẨM
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500)
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG NHÀ CUNG CẤP
-- ============================================================
CREATE TABLE IF NOT EXISTS suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(200) NOT NULL,
    phone   VARCHAR(30),
    email   VARCHAR(150),
    address VARCHAR(300)
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG SẢN PHẨM
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    category_id   BIGINT,
    unit          VARCHAR(30)  NOT NULL DEFAULT 'cái',
    import_price  DECIMAL(15,2) NOT NULL DEFAULT 0,
    sell_price    DECIMAL(15,2) NOT NULL DEFAULT 0,
    min_stock     INT NOT NULL DEFAULT 0,
    description   VARCHAR(500),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG TỒN KHO (1-1 với products, tách riêng để dễ lock khi cập nhật)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory (
    product_id  BIGINT PRIMARY KEY,
    quantity    INT NOT NULL DEFAULT 0,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_inventory_qty CHECK (quantity >= 0)
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG PHIẾU NHẬP KHO
-- ============================================================
CREATE TABLE IF NOT EXISTS import_receipts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50) NOT NULL UNIQUE,
    supplier_id      BIGINT,
    created_by       BIGINT NOT NULL,
    note             VARCHAR(500),
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_import_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS import_receipt_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_receipt_id   BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    quantity             INT NOT NULL,
    price                DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_iri_receipt FOREIGN KEY (import_receipt_id) REFERENCES import_receipts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_iri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_iri_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ============================================================
-- BẢNG PHIẾU XUẤT KHO
-- ============================================================
CREATE TABLE IF NOT EXISTS export_receipts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50) NOT NULL UNIQUE,
    customer_name    VARCHAR(200),
    created_by       BIGINT NOT NULL,
    note             VARCHAR(500),
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_export_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS export_receipt_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    export_receipt_id   BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    quantity             INT NOT NULL,
    price                DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_eri_receipt FOREIGN KEY (export_receipt_id) REFERENCES export_receipts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_eri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_eri_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ============================================================
-- DỮ LIỆU MẪU: tài khoản admin mặc định
-- Username: admin / Password: Admin@123
-- (hash bên dưới được tạo bằng BCrypt, cost factor 10)
-- ============================================================
INSERT INTO users (username, password_hash, full_name, role, active)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE username = username;

-- Lưu ý: hash mẫu trên tương ứng với password "secret".
-- Sau khi cài đặt, hãy chạy lớp PasswordHashGenerator (trong server)
-- để tạo hash thật cho mật khẩu bạn muốn, rồi UPDATE lại bảng users.
