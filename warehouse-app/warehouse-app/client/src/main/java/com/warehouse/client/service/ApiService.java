package com.warehouse.client.service;

import com.warehouse.shared.dto.*;
import com.warehouse.shared.protocol.Actions;
import com.warehouse.shared.protocol.Request;
import com.warehouse.shared.protocol.Response;
import com.warehouse.shared.util.JsonUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import com.google.gson.reflect.TypeToken;

/**
 * Client Service Layer: bao bọc NetworkClient, cung cấp các method nghiệp vụ cụ thể
 * (login, listProducts, createImportReceipt...) cho UI gọi, thay vì để UI tự build
 * Request/Action/JSON. Mọi method ở đây là blocking (đồng bộ) - UI layer (JavaFX)
 * có trách nhiệm gọi chúng từ background thread (Task/Thread), không gọi trực tiếp
 * trên JavaFX Application Thread để tránh đứng giao diện.
 */
public class ApiService {

    private final NetworkClient networkClient;

    public ApiService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void connect() throws IOException {
        networkClient.connect();
    }

    public boolean isConnected() {
        return networkClient.isConnected();
    }

    public void disconnect() {
        networkClient.disconnect();
    }

    // ================= AUTH =================

    public LoginResultDTO login(String username, String password) {
        LoginRequestDTO payload = new LoginRequestDTO(username, password);
        Response resp = execute(Actions.AUTH_LOGIN, payload, false);
        return JsonUtil.fromJson(resp.getData(), LoginResultDTO.class);
    }

    public void logout() {
        execute(Actions.AUTH_LOGOUT, null, true);
    }

    public void changePassword(String oldPassword, String newPassword) {
        execute(Actions.AUTH_CHANGE_PASSWORD, new ChangePasswordRequestDTO(oldPassword, newPassword), true);
    }

    // ================= USER =================

    public List<UserDTO> listUsers() {
        Response resp = execute(Actions.USER_LIST, null, true);
        return parseList(resp.getData(), UserDTO.class);
    }

    public UserDTO createUser(String username, String password, String fullName, String role) {
        Response resp = execute(Actions.USER_CREATE, new UserCreateRequestDTO(username, password, fullName, role), true);
        return JsonUtil.fromJson(resp.getData(), UserDTO.class);
    }

    public void updateUser(Long id, String fullName, String role, boolean active) {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO();
        dto.setId(id);
        dto.setFullName(fullName);
        dto.setRole(role);
        dto.setActive(active);
        execute(Actions.USER_UPDATE, dto, true);
    }

    public void deleteUser(Long id) {
        execute(Actions.USER_DELETE, new IdRequestDTO(id), true);
    }

    public void resetUserPassword(Long userId, String newPassword) {
        execute(Actions.USER_RESET_PASSWORD, new ResetPasswordRequestDTO(userId, newPassword), true);
    }

    // ================= CATEGORY =================

    public List<CategoryDTO> listCategories() {
        Response resp = execute(Actions.CATEGORY_LIST, null, true);
        return parseList(resp.getData(), CategoryDTO.class);
    }

    public CategoryDTO createCategory(String name, String description) {
        Response resp = execute(Actions.CATEGORY_CREATE, new CategoryDTO(null, name, description), true);
        return JsonUtil.fromJson(resp.getData(), CategoryDTO.class);
    }

    public void updateCategory(Long id, String name, String description) {
        execute(Actions.CATEGORY_UPDATE, new CategoryDTO(id, name, description), true);
    }

    public void deleteCategory(Long id) {
        execute(Actions.CATEGORY_DELETE, new IdRequestDTO(id), true);
    }

    // ================= SUPPLIER =================

    public List<SupplierDTO> listSuppliers() {
        Response resp = execute(Actions.SUPPLIER_LIST, null, true);
        return parseList(resp.getData(), SupplierDTO.class);
    }

    public SupplierDTO createSupplier(String name, String phone, String email, String address) {
        Response resp = execute(Actions.SUPPLIER_CREATE, new SupplierDTO(null, name, phone, email, address), true);
        return JsonUtil.fromJson(resp.getData(), SupplierDTO.class);
    }

    public void updateSupplier(Long id, String name, String phone, String email, String address) {
        execute(Actions.SUPPLIER_UPDATE, new SupplierDTO(id, name, phone, email, address), true);
    }

    public void deleteSupplier(Long id) {
        execute(Actions.SUPPLIER_DELETE, new IdRequestDTO(id), true);
    }

    // ================= PRODUCT =================

    public List<ProductDTO> listProducts() {
        Response resp = execute(Actions.PRODUCT_LIST, null, true);
        return parseList(resp.getData(), ProductDTO.class);
    }

    public List<ProductDTO> searchProducts(String keyword) {
        Response resp = execute(Actions.PRODUCT_SEARCH, new KeywordRequestDTO(keyword), true);
        return parseList(resp.getData(), ProductDTO.class);
    }

    public ProductDTO getProduct(Long id) {
        Response resp = execute(Actions.PRODUCT_GET, new IdRequestDTO(id), true);
        return JsonUtil.fromJson(resp.getData(), ProductDTO.class);
    }

    public ProductDTO createProduct(ProductDTO dto) {
        Response resp = execute(Actions.PRODUCT_CREATE, dto, true);
        return JsonUtil.fromJson(resp.getData(), ProductDTO.class);
    }

    public void updateProduct(ProductDTO dto) {
        execute(Actions.PRODUCT_UPDATE, dto, true);
    }

    public void deleteProduct(Long id) {
        execute(Actions.PRODUCT_DELETE, new IdRequestDTO(id), true);
    }

    // ================= IMPORT =================

    public List<ImportReceiptDTO> listImportReceipts() {
        Response resp = execute(Actions.IMPORT_LIST, null, true);
        return parseList(resp.getData(), ImportReceiptDTO.class);
    }

    public ImportReceiptDTO getImportReceipt(Long id) {
        Response resp = execute(Actions.IMPORT_GET, new IdRequestDTO(id), true);
        return JsonUtil.fromJson(resp.getData(), ImportReceiptDTO.class);
    }

    public ImportReceiptDTO createImportReceipt(ImportReceiptDTO dto) {
        Response resp = execute(Actions.IMPORT_CREATE, dto, true);
        return JsonUtil.fromJson(resp.getData(), ImportReceiptDTO.class);
    }

    public void deleteImportReceipt(Long id) {
        execute(Actions.IMPORT_DELETE, new IdRequestDTO(id), true);
    }

    // ================= EXPORT =================

    public List<ExportReceiptDTO> listExportReceipts() {
        Response resp = execute(Actions.EXPORT_LIST, null, true);
        return parseList(resp.getData(), ExportReceiptDTO.class);
    }

    public ExportReceiptDTO getExportReceipt(Long id) {
        Response resp = execute(Actions.EXPORT_GET, new IdRequestDTO(id), true);
        return JsonUtil.fromJson(resp.getData(), ExportReceiptDTO.class);
    }

    public ExportReceiptDTO createExportReceipt(ExportReceiptDTO dto) {
        Response resp = execute(Actions.EXPORT_CREATE, dto, true);
        return JsonUtil.fromJson(resp.getData(), ExportReceiptDTO.class);
    }

    public void deleteExportReceipt(Long id) {
        execute(Actions.EXPORT_DELETE, new IdRequestDTO(id), true);
    }

    // ================= INVENTORY =================

    public List<InventoryDTO> listInventory() {
        Response resp = execute(Actions.INVENTORY_LIST, null, true);
        return parseList(resp.getData(), InventoryDTO.class);
    }

    public List<InventoryDTO> listLowStock() {
        Response resp = execute(Actions.INVENTORY_LOW_STOCK, null, true);
        return parseList(resp.getData(), InventoryDTO.class);
    }

    // ================= INTERNAL =================

    private Response execute(String action, Object payload, boolean withToken) {
        String token = withToken ? SessionContext.getInstance().getToken() : null;
        String payloadJson = payload == null ? null : JsonUtil.toJson(payload);
        Request request = new Request(action, token, payloadJson);
        try {
            Response response = networkClient.send(request);
            if (!response.isSuccess()) {
                throw new ApiException(response.getMessage(), response.getErrorCode());
            }
            return response;
        } catch (IOException e) {
            throw new ApiException("Mất kết nối tới server: " + e.getMessage(), "NETWORK_ERROR");
        }
    }

    private <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return JsonUtil.gson().fromJson(json, listType);
    }
}
