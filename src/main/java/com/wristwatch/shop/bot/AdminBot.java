package com.wristwatch.shop.bot;

import com.wristwatch.shop.dto.*;
import com.wristwatch.shop.entity.Order;
import com.wristwatch.shop.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AdminBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.admin.token}")
    private String botToken;

    @Value("${telegram.bot.admin.username}")
    private String botUsername;

    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;
    private final AppUserService appUserService;
    private final StatisticsService statisticsService;
    private final TelegramFileService telegramFileService;

    // Admin session storage
    private final Map<Long, AdminSession> adminSessions = new ConcurrentHashMap<>();

    public AdminBot(CategoryService categoryService, ProductService productService,
                    OrderService orderService, AppUserService appUserService,
                    StatisticsService statisticsService, TelegramFileService telegramFileService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.orderService = orderService;
        this.appUserService = appUserService;
        this.statisticsService = statisticsService;
        this.telegramFileService = telegramFileService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            log.error("Error processing admin update", e);
        }
    }

    private void handleMessage(Message message) throws TelegramApiException {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText();

        // Check if user is admin
        if (!appUserService.isAdmin(userId)) {
            sendMessage(chatId, "❌ Access denied. You are not authorized to use this bot.");
            return;
        }

        AdminSession session = adminSessions.computeIfAbsent(userId, k -> new AdminSession());

        if (text != null) {
            switch (text) {
                case "/start":
                    sendWelcomeMessage(chatId);
                    break;
                case "📦 Manage Products":
                    showProductManagement(chatId);
                    break;
                case "📋 Manage Orders":
                    showOrderManagement(chatId);
                    break;
                case "📊 View Statistics":
                    showStatistics(chatId);
                    break;
                case "🏷️ Manage Categories":
                    showCategoryManagement(chatId);
                    break;
                case "ℹ️ Help":
                    sendHelpMessage(chatId);
                    break;
                default:
                    handleAdminInput(chatId, userId, text, session);
                    break;
            }
        } else if (message.hasPhoto()) {
            handlePhotoUpload(chatId, userId, message, session);
        }
    }

    private void handleCallbackQuery(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        if (!appUserService.isAdmin(userId)) {
            return;
        }

        String[] parts = callbackData.split(":");
        String action = parts[0];

        switch (action) {
            case "add_product":
                startAddProduct(chatId, userId);
                break;
            case "edit_product":
                showProductsForEdit(chatId);
                break;
            case "delete_product":
                showProductsForDelete(chatId);
                break;
            case "edit_product_id":
                Long editProductId = Long.parseLong(parts[1]);
                startEditProduct(chatId, userId, editProductId);
                break;
            case "delete_product_id":
                Long deleteProductId = Long.parseLong(parts[1]);
                confirmDeleteProduct(chatId, deleteProductId);
                break;
            case "confirm_delete_product":
                Long confirmDeleteId = Long.parseLong(parts[1]);
                deleteProduct(chatId, confirmDeleteId);
                break;
            case "pending_orders":
                showPendingOrders(chatId);
                break;
            case "view_order":
                Long orderId = Long.parseLong(parts[1]);
                showOrderDetails(chatId, orderId);
                break;
            case "approve_payment":
                Long approveOrderId = Long.parseLong(parts[1]);
                approvePayment(chatId, userId, approveOrderId);
                break;
            case "reject_payment":
                Long rejectOrderId = Long.parseLong(parts[1]);
                rejectPayment(chatId, userId, rejectOrderId);
                break;
            case "add_category":
                startAddCategory(chatId, userId);
                break;
            case "edit_category":
                showCategoriesForEdit(chatId);
                break;
            case "edit_category_id":
                Long editCategoryId = Long.parseLong(parts[1]);
                startEditCategory(chatId, userId, editCategoryId);
                break;
            case "stats_daily":
                showDailyStats(chatId);
                break;
            case "stats_weekly":
                showWeeklyStats(chatId);
                break;
            case "stats_monthly":
                showMonthlyStats(chatId);
                break;
            case "top_products":
                showTopProducts(chatId);
                break;
        }
    }

    private void sendWelcomeMessage(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔧 Welcome to Wristwatch Shop Admin Panel!\n\n" +
                "Manage your store efficiently with these tools:\n\n" +
                "📦 Manage Products - Add, edit, delete products\n" +
                "📋 Manage Orders - View and verify payments\n" +
                "📊 View Statistics - Sales reports and analytics\n" +
                "🏷️ Manage Categories - Organize your products\n\n" +
                "Select an option from the menu below:");
        message.setReplyMarkup(createAdminKeyboard());
        execute(message);
    }

    private void sendHelpMessage(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔧 Admin Bot Help:\n\n" +
                "📦 Product Management:\n" +
                "• Add new products with photos\n" +
                "• Edit existing product details\n" +
                "• Delete products (soft delete)\n\n" +
                "📋 Order Management:\n" +
                "• View pending payment verifications\n" +
                "• Approve or reject payments\n" +
                "• Notify customers automatically\n\n" +
                "📊 Statistics:\n" +
                "• Daily, weekly, monthly sales\n" +
                "• Top-selling products\n" +
                "• Revenue reports\n\n" +
                "🏷️ Category Management:\n" +
                "• Add new categories\n" +
                "• Edit category details\n\n" +
                "Need technical support? Contact the development team.");
        execute(message);
    }

    private void showProductManagement(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📦 Product Management\n\nChoose an action:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add Product");
        addBtn.setCallbackData("add_product");
        rows.add(Arrays.asList(addBtn));

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText("✏️ Edit Product");
        editBtn.setCallbackData("edit_product");
        rows.add(Arrays.asList(editBtn));

        InlineKeyboardButton deleteBtn = new InlineKeyboardButton();
        deleteBtn.setText("🗑️ Delete Product");
        deleteBtn.setCallbackData("delete_product");
        rows.add(Arrays.asList(deleteBtn));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showOrderManagement(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 Order Management\n\nChoose an action:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton pendingBtn = new InlineKeyboardButton();
        pendingBtn.setText("🔍 Pending Verifications");
        pendingBtn.setCallbackData("pending_orders");
        rows.add(Arrays.asList(pendingBtn));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showStatistics(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📊 Statistics & Reports\n\nChoose a report:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton dailyBtn = new InlineKeyboardButton();
        dailyBtn.setText("📅 Daily Sales");
        dailyBtn.setCallbackData("stats_daily");
        rows.add(Arrays.asList(dailyBtn));

        InlineKeyboardButton weeklyBtn = new InlineKeyboardButton();
        weeklyBtn.setText("📆 Weekly Sales");
        weeklyBtn.setCallbackData("stats_weekly");
        rows.add(Arrays.asList(weeklyBtn));

        InlineKeyboardButton monthlyBtn = new InlineKeyboardButton();
        monthlyBtn.setText("🗓️ Monthly Sales");
        monthlyBtn.setCallbackData("stats_monthly");
        rows.add(Arrays.asList(monthlyBtn));

        InlineKeyboardButton topBtn = new InlineKeyboardButton();
        topBtn.setText("🏆 Top Products");
        topBtn.setCallbackData("top_products");
        rows.add(Arrays.asList(topBtn));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showCategoryManagement(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🏷️ Category Management\n\nChoose an action:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add Category");
        addBtn.setCallbackData("add_category");
        rows.add(Arrays.asList(addBtn));

        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText("✏️ Edit Category");
        editBtn.setCallbackData("edit_category");
        rows.add(Arrays.asList(editBtn));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void startAddProduct(Long chatId, Long userId) throws TelegramApiException {
        AdminSession session = adminSessions.get(userId);
        session.setCurrentAction(AdminAction.ADD_PRODUCT);
        session.setProductStep(ProductStep.NAME);
        session.setTempProduct(new ProductCreateRequest());

        sendMessage(chatId, "➕ Adding New Product\n\nStep 1/6: Enter product name:");
    }

    private void startAddCategory(Long chatId, Long userId) throws TelegramApiException {
        AdminSession session = adminSessions.get(userId);
        session.setCurrentAction(AdminAction.ADD_CATEGORY);
        session.setCategoryStep(CategoryStep.NAME);
        session.setTempCategory(new CategoryDto());

        sendMessage(chatId, "➕ Adding New Category\n\nStep 1/2: Enter category name:");
    }

    private void showPendingOrders(Long chatId) throws TelegramApiException {
        List<OrderDto> pendingOrders = orderService.getPendingVerificationOrders();

        if (pendingOrders.isEmpty()) {
            sendMessage(chatId, "✅ No pending payment verifications!");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔍 Pending Payment Verifications (" + pendingOrders.size() + "):");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (OrderDto order : pendingOrders) {
            InlineKeyboardButton orderBtn = new InlineKeyboardButton();
            orderBtn.setText("Order #" + order.getId() + " - $" + order.getTotalAmount());
            orderBtn.setCallbackData("view_order:" + order.getId());
            rows.add(Arrays.asList(orderBtn));
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showOrderDetails(Long chatId, Long orderId) throws TelegramApiException {
        OrderDto order = orderService.getOrderById(orderId);

        StringBuilder orderText = new StringBuilder();
        orderText.append("📋 Order Details #").append(order.getId()).append("\n\n");
        orderText.append("👤 Customer: ").append(order.getCustomerName()).append("\n");
        orderText.append("📱 Phone: ").append(order.getCustomerPhone()).append("\n");
        orderText.append("📍 [Open Location](")
                .append(order.getDeliveryAddress())
                .append(")\n");
        orderText.append("📅 Date: ").append(order.getCreatedAt().toLocalDate()).append("\n");
        orderText.append("💰 Total: $").append(order.getTotalAmount()).append("\n");
        orderText.append("📊 Status: ").append(order.getStatus()).append("\n\n");

        orderText.append("🛍️ Items:\n");
        for (OrderItemDto item : order.getOrderItems()) {
            orderText.append("• ").append(item.getProductName()).append("\n");
            orderText.append("  Qty: ").append(item.getQuantity());
            orderText.append(" × $").append(item.getUnitPrice());
            orderText.append(" = $").append(item.getTotalPrice()).append("\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(orderText.toString());


        if (order.getStatus() == Order.OrderStatus.AWAITING_VERIFICATION) {
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton approveBtn = new InlineKeyboardButton();
            approveBtn.setText("✅ Approve Payment");
            approveBtn.setCallbackData("approve_payment:" + orderId);

            InlineKeyboardButton rejectBtn = new InlineKeyboardButton();
            rejectBtn.setText("❌ Reject Payment");
            rejectBtn.setCallbackData("reject_payment:" + orderId);

            rows.add(Arrays.asList(approveBtn, rejectBtn));
            keyboard.setKeyboard(rows);
            message.setReplyMarkup(keyboard);
        }

        execute(message);

        if (order.getStatus() == Order.OrderStatus.AWAITING_VERIFICATION && order.isHasPaymentProof()) {
            sendPaymentProofPhoto(chatId, orderId);
        }
    }

    private void sendPaymentProofPhoto(Long chatId, Long orderId) {
        try {
            String paymentProofPath = orderService.getPaymentProofPath(orderId);
            if (paymentProofPath != null && !paymentProofPath.isEmpty()) {
                File photoFile = new File(paymentProofPath);
                if (photoFile.exists()) {
                    SendPhoto photo = new SendPhoto();
                    photo.setChatId(chatId);
                    photo.setPhoto(new InputFile(photoFile));
                    photo.setCaption("💳 Payment Proof for Order #" + orderId);
                    execute(photo);
                } else {
                    sendMessage(chatId, "⚠️ Payment proof file not found on server.");
                }
            }
        } catch (Exception e) {
            log.error("Error sending payment proof photo for order " + orderId, e);
            try {
                sendMessage(chatId, "⚠️ Could not load payment proof image.");
            } catch (TelegramApiException ex) {
                log.error("Error sending error message", ex);
            }
        }
    }

    private void approvePayment(Long chatId, Long userId, Long orderId) throws TelegramApiException {
        try {
            orderService.verifyPayment(orderId, true, userId);
            sendMessage(chatId, "✅ Payment approved! Customer has been notified.");
        } catch (Exception e) {
            sendMessage(chatId, "❌ Error approving payment: " + e.getMessage());
        }
    }

    private void rejectPayment(Long chatId, Long userId, Long orderId) throws TelegramApiException {
        try {
            orderService.verifyPayment(orderId, false, userId);
            sendMessage(chatId, "❌ Payment rejected! Customer has been notified.");
        } catch (Exception e) {
            sendMessage(chatId, "❌ Error rejecting payment: " + e.getMessage());
        }
    }

    private void showDailyStats(Long chatId) throws TelegramApiException {
        Map<String, Object> stats = statisticsService.getDailySales();

        String statsText = "📅 Daily Sales Report\n\n" +
                "📦 Orders: " + stats.get("orderCount") + "\n" +
                "💰 Revenue: $" + String.format("%.2f", (Double) stats.get("totalRevenue")) + "\n" +
                "📊 Period: Today";

        sendMessage(chatId, statsText);
    }

    private void showWeeklyStats(Long chatId) throws TelegramApiException {
        Map<String, Object> stats = statisticsService.getWeeklySales();

        String statsText = "📆 Weekly Sales Report\n\n" +
                "📦 Orders: " + stats.get("orderCount") + "\n" +
                "💰 Revenue: $" + String.format("%.2f", (Double) stats.get("totalRevenue")) + "\n" +
                "📊 Period: Last 7 days";

        sendMessage(chatId, statsText);
    }

    private void showMonthlyStats(Long chatId) throws TelegramApiException {
        Map<String, Object> stats = statisticsService.getMonthlySales();

        String statsText = "🗓️ Monthly Sales Report\n\n" +
                "📦 Orders: " + stats.get("orderCount") + "\n" +
                "💰 Revenue: $" + String.format("%.2f", (Double) stats.get("totalRevenue")) + "\n" +
                "📊 Period: Last 30 days";

        sendMessage(chatId, statsText);
    }

    private void showTopProducts(Long chatId) throws TelegramApiException {
        List<Object[]> topProducts = statisticsService.getTopSellingProducts(30);

        if (topProducts.isEmpty()) {
            sendMessage(chatId, "📊 No sales data available for the last 30 days.");
            return;
        }

        StringBuilder statsText = new StringBuilder("🏆 Top Selling Products (Last 30 days)\n\n");

        int rank = 1;
        for (Object[] product : topProducts) {
            String productName = (String) product[0];
            Long totalSold = (Long) product[1];

            statsText.append(rank).append(". ").append(productName).append("\n");
            statsText.append("   Sold: ").append(totalSold).append(" units\n\n");

            rank++;
            if (rank > 10) break; // Show top 10
        }

        sendMessage(chatId, statsText.toString());
    }

    private void handleAdminInput(Long chatId, Long userId, String text, AdminSession session) throws TelegramApiException {
        if (session.getCurrentAction() == AdminAction.ADD_PRODUCT) {
            handleAddProductInput(chatId, userId, text, session);
        } else if (session.getCurrentAction() == AdminAction.ADD_CATEGORY) {
            handleAddCategoryInput(chatId, userId, text, session);
        } else {
            sendMessage(chatId, "I don't understand that command. Use the menu buttons or type /start.");
        }
    }

    private void handleAddProductInput(Long chatId, Long userId, String text, AdminSession session) throws TelegramApiException {
        ProductCreateRequest product = session.getTempProduct();

        switch (session.getProductStep()) {
            case NAME:
                product.setName(text);
                session.setProductStep(ProductStep.DESCRIPTION);
                sendMessage(chatId, "Step 2/6: Enter product description:");
                break;
            case DESCRIPTION:
                product.setDescription(text);
                session.setProductStep(ProductStep.PRICE);
                sendMessage(chatId, "Step 3/6: Enter product price (e.g., 299.99):");
                break;
            case PRICE:
                try {
                    product.setPrice(new BigDecimal(text));
                    session.setProductStep(ProductStep.STOCK);
                    sendMessage(chatId, "Step 4/6: Enter stock quantity:");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Invalid price format. Please enter a valid number:");
                }
                break;
            case STOCK:
                try {
                    product.setStock(Integer.parseInt(text));
                    session.setProductStep(ProductStep.CATEGORY);
                    showCategoriesForProduct(chatId);
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Invalid stock format. Please enter a valid number:");
                }
                break;
            case CATEGORY:
                try {
                    product.setCategoryId(Long.parseLong(text));
                    session.setProductStep(ProductStep.IMAGE);
                    sendMessage(chatId, "Step 6/6: Send product image or type 'skip' to finish without image:");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Invalid category ID. Please enter a valid number:");
                }
                break;
            case IMAGE:
                if ("skip".equalsIgnoreCase(text)) {
                    saveProduct(chatId, session);
                } else {
                    sendMessage(chatId, "Please send an image or type 'skip':");
                }
                break;
        }
    }

    private void handleAddCategoryInput(Long chatId, Long userId, String text, AdminSession session) throws TelegramApiException {
        CategoryDto category = session.getTempCategory();

        switch (session.getCategoryStep()) {
            case NAME:
                category.setName(text);
                session.setCategoryStep(CategoryStep.DESCRIPTION);
                sendMessage(chatId, "Step 2/2: Enter category description (or type 'skip'):");
                break;
            case DESCRIPTION:
                if (!"skip".equalsIgnoreCase(text)) {
                    category.setDescription(text);
                }
                saveCategory(chatId, session);
                break;
        }
    }

    private void showCategoriesForProduct(Long chatId) throws TelegramApiException {
        List<CategoryDto> categories = categoryService.getAllCategories();

        StringBuilder text = new StringBuilder("Step 5/6: Select category by entering its ID:\n\n");
        for (CategoryDto category : categories) {
            text.append(category.getId()).append(". ").append(category.getName()).append("\n");
        }

        sendMessage(chatId, text.toString());
    }

    private void handlePhotoUpload(Long chatId, Long userId, Message message, AdminSession session) throws TelegramApiException {
        if (session.getCurrentAction() == AdminAction.ADD_PRODUCT &&
                session.getProductStep() == ProductStep.IMAGE) {

            try {
                String imageUrl = telegramFileService.downloadAndStorePhoto(message, this, "products");
                session.getTempProduct().setImageUrl(imageUrl);

                sendMessage(chatId, "✅ Image uploaded successfully!");
                saveProduct(chatId, session);
            } catch (Exception e) {
                log.error("Error uploading product image", e);
                sendMessage(chatId, "❌ Error uploading image: " + e.getMessage() + "\nProduct will be saved without image.");
                saveProduct(chatId, session);
            }
        } else {
            sendMessage(chatId, "I'm not expecting an image right now.");
        }
    }

    private void saveProduct(Long chatId, AdminSession session) throws TelegramApiException {
        try {
            ProductDto savedProduct = productService.createProduct(session.getTempProduct());
            sendMessage(chatId, "✅ Product '" + savedProduct.getName() + "' added successfully!");

            // Reset session
            session.setCurrentAction(null);
            session.setProductStep(null);
            session.setTempProduct(null);

        } catch (Exception e) {
            sendMessage(chatId, "❌ Error adding product: " + e.getMessage());
        }
    }

    private void saveCategory(Long chatId, AdminSession session) throws TelegramApiException {
        try {
            CategoryDto savedCategory = categoryService.createCategory(session.getTempCategory());
            sendMessage(chatId, "✅ Category '" + savedCategory.getName() + "' added successfully!");

            // Reset session
            session.setCurrentAction(null);
            session.setCategoryStep(null);
            session.setTempCategory(null);

        } catch (Exception e) {
            sendMessage(chatId, "❌ Error adding category: " + e.getMessage());
        }
    }

    private void showProductsForEdit(Long chatId) throws TelegramApiException {
        List<ProductDto> products = productService.getAllProducts();

        if (products.isEmpty()) {
            sendMessage(chatId, "No products available to edit.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a product to edit:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ProductDto product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() + " - $" + product.getPrice());
            button.setCallbackData("edit_product_id:" + product.getId());
            rows.add(Arrays.asList(button));
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showProductsForDelete(Long chatId) throws TelegramApiException {
        List<ProductDto> products = productService.getAllProducts();

        if (products.isEmpty()) {
            sendMessage(chatId, "No products available to delete.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚠️ Select a product to delete:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ProductDto product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() + " - $" + product.getPrice());
            button.setCallbackData("delete_product_id:" + product.getId());
            rows.add(Arrays.asList(button));
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void confirmDeleteProduct(Long chatId, Long productId) throws TelegramApiException {
        ProductDto product = productService.getProductById(productId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚠️ Are you sure you want to delete this product?\n\n" +
                "📦 " + product.getName() + "\n" +
                "💰 $" + product.getPrice() + "\n\n" +
                "This action cannot be undone!");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("🗑️ Yes, Delete");
        confirmBtn.setCallbackData("confirm_delete_product:" + productId);
        rows.add(Arrays.asList(confirmBtn));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void deleteProduct(Long chatId, Long productId) throws TelegramApiException {
        try {
            productService.deleteProduct(productId);
            sendMessage(chatId, "✅ Product deleted successfully!");
        } catch (Exception e) {
            sendMessage(chatId, "❌ Error deleting product: " + e.getMessage());
        }
    }

    private void showCategoriesForEdit(Long chatId) throws TelegramApiException {
        List<CategoryDto> categories = categoryService.getAllCategories();

        if (categories.isEmpty()) {
            sendMessage(chatId, "No categories available to edit.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a category to edit:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (CategoryDto category : categories) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category.getName());
            button.setCallbackData("edit_category_id:" + category.getId());
            rows.add(Arrays.asList(button));
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void startEditProduct(Long chatId, Long userId, Long productId) throws TelegramApiException {
        // Implementation for editing products would go here
        sendMessage(chatId, "Product editing feature coming soon!");
    }

    private void startEditCategory(Long chatId, Long userId, Long categoryId) throws TelegramApiException {
        // Implementation for editing categories would go here
        sendMessage(chatId, "Category editing feature coming soon!");
    }

    private ReplyKeyboardMarkup createAdminKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📦 Manage Products"));
        row1.add(new KeyboardButton("📋 Manage Orders"));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📊 View Statistics"));
        row2.add(new KeyboardButton("🏷️ Manage Categories"));
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("ℹ️ Help"));
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }
}
