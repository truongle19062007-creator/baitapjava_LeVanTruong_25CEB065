package com.warehouse.server.handler;

import com.google.gson.reflect.TypeToken;
import com.warehouse.server.security.Session;
import com.warehouse.server.service.*;
import com.warehouse.shared.dto.*;
import com.warehouse.shared.protocol.Actions;
import com.warehouse.shared.protocol.Request;
import com.warehouse.shared.protocol.Response;
import com.warehouse.shared.util.JsonUtil;
import com.warehouse.server.util.DtoMapper;
import com.warehouse.server.model.User;
import com.warehouse.server.model.Category;
import com.warehouse.server.model.Supplier;
import com.warehouse.server.model.Product;
import com.warehouse.server.model.ImportReceipt;
import com.warehouse.server.model.ExportReceipt;
import com.warehouse.server.model.ReceiptItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Bộ định tuyến request: nhận Request đã parse, xác định action, kiểm tra
 * xác thực/quyền hạn cần thiết, gọi Service tương ứng, rồi đóng gói kết quả thành Response.
 *
 * Mỗi instance của RequestRouter được dùng lại cho nhiều client (stateless ngoại trừ các
 * Service/DAO bên trong, mà bản thân chúng cũng stateless và thread-safe), nên 1 instance
 * có thể share giữa tất cả ClientHandler thread.
 */
public class RequestRouter {

    private final AuthService authService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final ImportService importService;
    private final ExportService exportService;
    private final InventoryService inventoryService;

    public RequestRouter(AuthService authService, UserService userService, CategoryService categoryService,
                          SupplierService supplierService, ProductService productService,
                          ImportService importService, ExportService exportService,
                          InventoryService inventoryService) {
        this.authService = authService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.importService = importService;
        this.exportService = exportService;
        this.inventoryService = inventoryService;
    }

    public Response handle(Request request) {
        try {
            String action = request.getAction();
            if (action == null) {
                return Response.fail("Thiếu action trong request", "BAD_REQUEST");
            }

            switch (action) {
                // ===== AUTH =====
                case Actions.AUTH_LOGIN:
                    return handleLogin(request);
                case Actions.AUTH_LOGOUT:
                    authService.logout(request.getToken());
                    return Response.ok("Đăng xuất thành công");
                case Actions.AUTH_CHANGE_PASSWORD:
                    return handleChangePassword(request);

                // ===== USER (chỉ ADMIN) =====
                case Actions.USER_LIST:
                    return handleUserList(request);
                case Actions.USER_CREATE:
                    return handleUserCreate(request);
                case Actions.USER_UPDATE:
                    return handleUserUpdate(request);
                case Actions.USER_DELETE:
                    return handleUserDelete(request);
                case Actions.USER_RESET_PASSWORD:
                    return handleUserResetPassword(request);

                // ===== CATEGORY =====
                case Actions.CATEGORY_LIST:
                    return handleCategoryList(request);
                case Actions.CATEGORY_CREATE:
                    return handleCategoryCreate(request);
                case Actions.CATEGORY_UPDATE:
                    return handleCategoryUpdate(request);
                case Actions.CATEGORY_DELETE:
                    return handleCategoryDelete(request);

                // ===== SUPPLIER =====
                case Actions.SUPPLIER_LIST:
                    return handleSupplierList(request);
                case Actions.SUPPLIER_CREATE:
                    return handleSupplierCreate(request);
                case Actions.SUPPLIER_UPDATE:
                    return handleSupplierUpdate(request);
                case Actions.SUPPLIER_DELETE:
                    return handleSupplierDelete(request);

                // ===== PRODUCT =====
                case Actions.PRODUCT_LIST:
                    return handleProductList(request);
                case Actions.PRODUCT_SEARCH:
                    return handleProductSearch(request);
                case Actions.PRODUCT_GET:
                    return handleProductGet(request);
                case Actions.PRODUCT_CREATE:
                    return handleProductCreate(request);
                case Actions.PRODUCT_UPDATE:
                    return handleProductUpdate(request);
                case Actions.PRODUCT_DELETE:
                    return handleProductDelete(request);

                // ===== IMPORT =====
                case Actions.IMPORT_LIST:
                    return handleImportList(request);
                case Actions.IMPORT_GET:
                    return handleImportGet(request);
                case Actions.IMPORT_CREATE:
                    return handleImportCreate(request);
                case Actions.IMPORT_DELETE:
                    return handleImportDelete(request);

                // ===== EXPORT =====
                case Actions.EXPORT_LIST:
                    return handleExportList(request);
                case Actions.EXPORT_GET:
                    return handleExportGet(request);
                case Actions.EXPORT_CREATE:
                    return handleExportCreate(request);
                case Actions.EXPORT_DELETE:
                    return handleExportDelete(request);

                // ===== INVENTORY =====
                case Actions.INVENTORY_LIST:
                    return handleInventoryList(request);
                case Actions.INVENTORY_LOW_STOCK:
                    return handleInventoryLowStock(request);

                default:
                    return Response.fail("Action không được hỗ trợ: " + action, "UNKNOWN_ACTION");
            }
        } catch (BusinessException e) {
            return Response.fail(e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            // Không lộ chi tiết exception hệ thống (stack trace, SQL...) cho client.
            // Log lại ở server để debug.
            System.err.println("[RequestRouter] Lỗi không mong muốn xử lý action=" + request.getAction());
            e.printStackTrace();
            return Response.fail("Đã có lỗi xảy ra ở server. Vui lòng thử lại sau", "INTERNAL_ERROR");
        }
    }

    // ================= AUTH =================

    private Response handleLogin(Request req) {
        LoginRequestDTO dto = JsonUtil.fromJson(req.getPayload(), LoginRequestDTO.class);
        if (dto == null) {
            return Response.fail("Dữ liệu đăng nhập không hợp lệ", "BAD_REQUEST");
        }
        Session session = authService.login(dto.getUsername(), dto.getPassword());
        LoginResultDTO result = new LoginResultDTO(session.getToken(), DtoMapper.toDto(session.getUser()));
        return Response.ok("Đăng nhập thành công", JsonUtil.toJson(result));
    }

    private Response handleChangePassword(Request req) {
        Session session = authService.requireSession(req.getToken());
        ChangePasswordRequestDTO dto = JsonUtil.fromJson(req.getPayload(), ChangePasswordRequestDTO.class);
        if (dto == null) {
            return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        }
        authService.changePassword(session, dto.getOldPassword(), dto.getNewPassword());
        return Response.ok("Đổi mật khẩu thành công");
    }

    // ================= USER =================

    private Response handleUserList(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN");
        List<User> users = userService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toUserDtoList(users)));
    }

    private Response handleUserCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN");
        UserCreateRequestDTO dto = JsonUtil.fromJson(req.getPayload(), UserCreateRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        User created = userService.create(dto.getUsername(), dto.getPassword(), dto.getFullName(), dto.getRole());
        return Response.ok("Tạo người dùng thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleUserUpdate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN");
        UserUpdateRequestDTO dto = JsonUtil.fromJson(req.getPayload(), UserUpdateRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        userService.update(dto.getId(), dto.getFullName(), dto.getRole(), dto.isActive());
        return Response.ok("Cập nhật người dùng thành công");
    }

    private Response handleUserDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        userService.delete(dto.getId(), session.getUser().getId());
        return Response.ok("Xoá người dùng thành công");
    }

    private Response handleUserResetPassword(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN");
        ResetPasswordRequestDTO dto = JsonUtil.fromJson(req.getPayload(), ResetPasswordRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        userService.resetPassword(dto.getUserId(), dto.getNewPassword());
        return Response.ok("Đặt lại mật khẩu thành công");
    }

    // ================= CATEGORY =================

    private Response handleCategoryList(Request req) {
        authService.requireSession(req.getToken());
        List<Category> list = categoryService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toCategoryDtoList(list)));
    }

    private Response handleCategoryCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        CategoryDTO dto = JsonUtil.fromJson(req.getPayload(), CategoryDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        Category created = categoryService.create(dto.getName(), dto.getDescription());
        return Response.ok("Tạo danh mục thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleCategoryUpdate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        CategoryDTO dto = JsonUtil.fromJson(req.getPayload(), CategoryDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        categoryService.update(dto.getId(), dto.getName(), dto.getDescription());
        return Response.ok("Cập nhật danh mục thành công");
    }

    private Response handleCategoryDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        categoryService.delete(dto.getId());
        return Response.ok("Xoá danh mục thành công");
    }

    // ================= SUPPLIER =================

    private Response handleSupplierList(Request req) {
        authService.requireSession(req.getToken());
        List<Supplier> list = supplierService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toSupplierDtoList(list)));
    }

    private Response handleSupplierCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        SupplierDTO dto = JsonUtil.fromJson(req.getPayload(), SupplierDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        Supplier created = supplierService.create(dto.getName(), dto.getPhone(), dto.getEmail(), dto.getAddress());
        return Response.ok("Tạo nhà cung cấp thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleSupplierUpdate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        SupplierDTO dto = JsonUtil.fromJson(req.getPayload(), SupplierDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        supplierService.update(dto.getId(), dto.getName(), dto.getPhone(), dto.getEmail(), dto.getAddress());
        return Response.ok("Cập nhật nhà cung cấp thành công");
    }

    private Response handleSupplierDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        supplierService.delete(dto.getId());
        return Response.ok("Xoá nhà cung cấp thành công");
    }

    // ================= PRODUCT =================

    private Response handleProductList(Request req) {
        authService.requireSession(req.getToken());
        List<Product> list = productService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toProductDtoList(list)));
    }

    private Response handleProductSearch(Request req) {
        authService.requireSession(req.getToken());
        KeywordRequestDTO dto = JsonUtil.fromJson(req.getPayload(), KeywordRequestDTO.class);
        String keyword = dto == null ? null : dto.getKeyword();
        List<Product> list = productService.search(keyword);
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toProductDtoList(list)));
    }

    private Response handleProductGet(Request req) {
        authService.requireSession(req.getToken());
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        Product p = productService.get(dto.getId());
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toDto(p)));
    }

    private Response handleProductCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        ProductDTO dto = JsonUtil.fromJson(req.getPayload(), ProductDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        Product created = productService.create(dto.getCode(), dto.getName(), dto.getCategoryId(), dto.getUnit(),
                dto.getImportPrice(), dto.getSellPrice(), dto.getMinStock(), dto.getDescription());
        return Response.ok("Tạo sản phẩm thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleProductUpdate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        ProductDTO dto = JsonUtil.fromJson(req.getPayload(), ProductDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        productService.update(dto.getId(), dto.getName(), dto.getCategoryId(), dto.getUnit(),
                dto.getImportPrice(), dto.getSellPrice(), dto.getMinStock(), dto.getDescription());
        return Response.ok("Cập nhật sản phẩm thành công");
    }

    private Response handleProductDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        productService.delete(dto.getId());
        return Response.ok("Xoá sản phẩm thành công");
    }

    // ================= IMPORT =================

    private Response handleImportList(Request req) {
        authService.requireSession(req.getToken());
        List<ImportReceipt> list = importService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toImportDtoList(list)));
    }

    private Response handleImportGet(Request req) {
        authService.requireSession(req.getToken());
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        ImportReceipt r = importService.get(dto.getId());
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toDto(r)));
    }

    private Response handleImportCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER", "STAFF");
        ImportReceiptDTO dto = JsonUtil.fromJson(req.getPayload(), ImportReceiptDTO.class);
        if (dto == null || dto.getItems() == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        List<ReceiptItem> items = DtoMapper.fromItemDtoList(dto.getItems());
        ImportReceipt created = importService.createReceipt(
                dto.getSupplierId(), session.getUser().getId(), dto.getNote(), items);
        return Response.ok("Tạo phiếu nhập kho thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleImportDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        importService.deleteReceipt(dto.getId());
        return Response.ok("Xoá phiếu nhập kho thành công");
    }

    // ================= EXPORT =================

    private Response handleExportList(Request req) {
        authService.requireSession(req.getToken());
        List<ExportReceipt> list = exportService.listAll();
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toExportDtoList(list)));
    }

    private Response handleExportGet(Request req) {
        authService.requireSession(req.getToken());
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        ExportReceipt r = exportService.get(dto.getId());
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toDto(r)));
    }

    private Response handleExportCreate(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER", "STAFF");
        ExportReceiptDTO dto = JsonUtil.fromJson(req.getPayload(), ExportReceiptDTO.class);
        if (dto == null || dto.getItems() == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        List<ReceiptItem> items = DtoMapper.fromItemDtoList(dto.getItems());
        ExportReceipt created = exportService.createReceipt(
                dto.getCustomerName(), session.getUser().getId(), dto.getNote(), items);
        return Response.ok("Tạo phiếu xuất kho thành công", JsonUtil.toJson(DtoMapper.toDto(created)));
    }

    private Response handleExportDelete(Request req) {
        Session session = authService.requireSession(req.getToken());
        authService.requireRole(session, "ADMIN", "MANAGER");
        IdRequestDTO dto = JsonUtil.fromJson(req.getPayload(), IdRequestDTO.class);
        if (dto == null) return Response.fail("Dữ liệu không hợp lệ", "BAD_REQUEST");
        exportService.deleteReceipt(dto.getId());
        return Response.ok("Xoá phiếu xuất kho thành công");
    }

    // ================= INVENTORY =================

    private Response handleInventoryList(Request req) {
        authService.requireSession(req.getToken());
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toInventoryDtoList(inventoryService.listAll())));
    }

    private Response handleInventoryLowStock(Request req) {
        authService.requireSession(req.getToken());
        return Response.ok(null, JsonUtil.toJson(DtoMapper.toInventoryDtoList(inventoryService.listLowStock())));
    }
}
