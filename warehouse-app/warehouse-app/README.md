# Ứng dụng Quản lý Kho — Java (JavaFX Client + Socket Server + MySQL)

## Kiến trúc

```
warehouse-app/
├── shared/   -> DTO + giao thức Request/Response dùng chung (JSON qua Socket)
├── server/   -> ServerSocket đa luồng, Service, DAO (JDBC), MySQL, bảo mật (BCrypt + token)
└── client/   -> JavaFX UI, gọi server qua Socket bằng JSON
```

Giao thức: client gửi 1 dòng JSON (`Request`) qua Socket, server trả về 1 dòng JSON (`Response`),
kết thúc bằng ký tự xuống dòng. Mỗi client được Server giao cho 1 thread riêng xử lý (thread pool,
xem `ServerMain`).

## Yêu cầu

- JDK 17+
- Maven 3.8+
- MySQL 8+

## 1. Khởi tạo Database

```bash
mysql -u root -p < server/src/main/resources/sql/schema.sql
```

Script tạo database `warehouse_db`, toàn bộ bảng, và 1 tài khoản `admin` mẫu.

**Quan trọng**: hash mật khẩu mẫu trong file `schema.sql` chỉ là ví dụ minh hoạ.
Hãy tạo hash thật cho mật khẩu bạn muốn dùng:

```bash
cd server
mvn compile exec:java -Dexec.mainClass="com.warehouse.server.security.PasswordHashGenerator" -Dexec.args="MatKhauCuaBan"
```

Lệnh trên in ra câu `UPDATE users SET password_hash = '...' WHERE username = 'admin';` — chạy câu UPDATE đó trong MySQL.

## 2. Cấu hình kết nối Database

Sửa file `server/src/main/resources/server.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
db.username=root
db.password=MAT_KHAU_MYSQL_CUA_BAN
server.port=9999
server.maxThreads=50
```

## 3. Build toàn bộ project

Từ thư mục gốc `warehouse-app/`:

```bash
mvn clean install
```

Lệnh này build `shared` trước, rồi `server`, rồi `client` (đúng thứ tự phụ thuộc).

## 4. Chạy Server

```bash
cd server
mvn exec:java -Dexec.mainClass="com.warehouse.server.ServerMain"
```

Hoặc dùng jar đã build (do `maven-shade-plugin` đóng gói kèm dependency):

```bash
java -jar server/target/server-1.0.0.jar
```

Nếu chạy bằng jar, đặt file `server.properties` cùng thư mục với jar để override cấu hình mà
không cần build lại.

Server log ra: `Server đang lắng nghe ở cổng 9999 ...`

## 5. Chạy Client (JavaFX)

```bash
cd client
mvn javafx:run
```

Hoặc chạy trực tiếp class `com.warehouse.client.MainApp` từ IDE (IntelliJ/Eclipse) sau khi đã
thêm JavaFX module vào VM options nếu cần (`--module-path` ... `--add-modules javafx.controls,javafx.fxml`),
plugin `javafx-maven-plugin` đã tự xử lý việc này khi chạy bằng `mvn javafx:run`.

Mặc định client kết nối tới `localhost:9999`. Có thể đổi qua system property:

```bash
mvn javafx:run -Dwarehouse.server.host=192.168.1.10 -Dwarehouse.server.port=9999
```

## 6. Đăng nhập

Tài khoản mẫu: `admin` / mật khẩu bạn đã set ở bước 1.

## Các module đã có

| Module       | Mô tả                                                              |
|--------------|---------------------------------------------------------------------|
| Auth         | Đăng nhập, đăng xuất, đổi mật khẩu, session token (in-memory)       |
| User         | CRUD người dùng, phân quyền ADMIN / MANAGER / STAFF (chỉ ADMIN)     |
| Category     | CRUD danh mục sản phẩm                                              |
| Supplier     | CRUD nhà cung cấp                                                   |
| Product      | CRUD sản phẩm, gắn danh mục, giá nhập/bán, ngưỡng tồn tối thiểu      |
| Import       | Tạo/xem/xoá phiếu nhập kho — tự động tăng tồn kho (transaction)      |
| Export       | Tạo/xem/xoá phiếu xuất kho — kiểm tra & trừ tồn kho (transaction)    |
| Inventory    | Xem tồn kho hiện tại, lọc cảnh báo tồn kho thấp                      |

## Điểm kỹ thuật quan trọng

- **Transaction & khoá hàng (row lock)**: `InventoryDAO.lockForUpdate()` dùng
  `SELECT ... FOR UPDATE` để tránh 2 phiếu xuất cùng lúc trừ vượt quá tồn kho thực tế
  (race condition khi nhiều client cùng giao dịch).
- **Bảo mật**: mật khẩu hash bằng BCrypt (`PasswordUtil`), không lưu plaintext.
  Token session sinh ngẫu nhiên 256-bit (`SessionManager`), hết hạn sau 30 phút không hoạt động.
  Server không lộ chi tiết lỗi hệ thống (SQL, stack trace) cho client — chỉ trả message nghiệp vụ.
- **Phân quyền**: `AuthService.requireRole()` kiểm tra role trước khi cho phép các hành động
  sửa/xoá dữ liệu nhạy cảm.
- **Đa luồng server**: mỗi client = 1 thread (lấy từ `ExecutorService` thread pool cố định),
  các Service/DAO không giữ state riêng (ngoại trừ `SessionManager` dùng `ConcurrentHashMap`)
  nên an toàn khi nhiều thread gọi đồng thời.

## Mở rộng thêm

- Thêm logging file (SLF4J + Logback) thay cho `System.out`.
- Thêm phân trang cho danh sách sản phẩm/phiếu khi dữ liệu lớn.
- Thêm báo cáo (doanh thu nhập/xuất theo khoảng thời gian) — có thể thêm action mới
  `REPORT.IMPORT_EXPORT_SUMMARY` theo đúng pattern hiện có.
- Nếu cần chạy nhiều server instance (load balancing), cần chuyển `SessionManager` từ
  in-memory sang Redis hoặc bảng DB riêng.
