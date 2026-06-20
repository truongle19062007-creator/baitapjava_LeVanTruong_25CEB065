package com.warehouse.server;

import com.warehouse.server.dao.*;
import com.warehouse.server.db.DatabaseManager;
import com.warehouse.server.handler.ClientHandler;
import com.warehouse.server.handler.RequestRouter;
import com.warehouse.server.security.SessionManager;
import com.warehouse.server.service.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point của Server Application.
 *
 * Luồng khởi động:
 *   1. Đọc cấu hình (DB url/user/pass, port) từ file server.properties (hoặc dùng default).
 *   2. Khởi tạo connection pool tới MySQL.
 *   3. Khởi tạo DAO -> Service -> RequestRouter (toàn bộ đều stateless/thread-safe,
 *      dùng lại 1 instance duy nhất cho tất cả client).
 *   4. Mở ServerSocket, lắng nghe kết nối. Mỗi client kết nối vào sẽ được giao cho
 *      1 thread riêng (lấy từ ExecutorService thread pool) để xử lý độc lập,
 *      không block các client khác.
 */
public class ServerMain {

    public static void main(String[] args) {
        Properties config = loadConfig();

        String jdbcUrl = config.getProperty("db.url", "jdbc:mysql://localhost:3306/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8");
        String dbUser = config.getProperty("db.username", "root");
        String dbPassword = config.getProperty("db.password", "");
        int port = Integer.parseInt(config.getProperty("server.port", "9999"));
        int maxThreads = Integer.parseInt(config.getProperty("server.maxThreads", "50"));

        System.out.println("=== WAREHOUSE SERVER ===");
        System.out.println("Đang kết nối database: " + jdbcUrl);
        DatabaseManager.init(jdbcUrl, dbUser, dbPassword);
        System.out.println("Kết nối database thành công.");

        // ----- Khởi tạo DAO -----
        UserDAO userDAO = new UserDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        SupplierDAO supplierDAO = new SupplierDAO();
        ProductDAO productDAO = new ProductDAO();
        InventoryDAO inventoryDAO = new InventoryDAO();
        ImportReceiptDAO importReceiptDAO = new ImportReceiptDAO();
        ExportReceiptDAO exportReceiptDAO = new ExportReceiptDAO();

        // ----- Khởi tạo Service -----
        SessionManager sessionManager = new SessionManager();
        AuthService authService = new AuthService(userDAO, sessionManager);
        UserService userService = new UserService(userDAO);
        CategoryService categoryService = new CategoryService(categoryDAO);
        SupplierService supplierService = new SupplierService(supplierDAO);
        ProductService productService = new ProductService(productDAO, inventoryDAO);
        ImportService importService = new ImportService(importReceiptDAO, inventoryDAO, productDAO);
        ExportService exportService = new ExportService(exportReceiptDAO, inventoryDAO, productDAO);
        InventoryService inventoryService = new InventoryService(inventoryDAO);

        RequestRouter requestRouter = new RequestRouter(
                authService, userService, categoryService, supplierService,
                productService, importService, exportService, inventoryService);

        ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);

        // Đảm bảo đóng connection pool gọn gàng khi server bị tắt (Ctrl+C, kill...)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ServerMain] Đang tắt server...");
            threadPool.shutdownNow();
            DatabaseManager.shutdown();
            System.out.println("[ServerMain] Đã tắt server.");
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server đang lắng nghe ở cổng " + port + " (tối đa " + maxThreads + " client đồng thời)...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket, requestRouter));
            }
        } catch (IOException e) {
            System.err.println("[ServerMain] Lỗi ServerSocket: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdownNow();
            DatabaseManager.shutdown();
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = ServerMain.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (is != null) {
                props.load(is);
                return props;
            }
        } catch (IOException ignored) {
        }
        // Thử đọc từ file ngoài classpath (cùng thư mục chạy jar), cho phép chỉnh cấu hình
        // mà không cần build lại jar.
        try (InputStream is = new FileInputStream("server.properties")) {
            props.load(is);
        } catch (IOException ignored) {
            System.out.println("Không tìm thấy server.properties, dùng cấu hình mặc định.");
        }
        return props;
    }
}
