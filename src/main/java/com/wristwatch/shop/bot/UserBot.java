package com.wristwatch.shop.bot;

import com.wristwatch.shop.dto.*;
import com.wristwatch.shop.entity.Order;
import com.wristwatch.shop.event.PaymentVerificationEvent;
import com.wristwatch.shop.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
public class UserBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.user.token}")
    private String botToken;

    @Value("${telegram.bot.user.username}")
    private String botUsername;

    @Value("${payment.merchant.details}")
    private String merchantDetails;

    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;
    private final AppUserService appUserService;
    private final TelegramFileService telegramFileService;

    // User session storage
    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    public UserBot(CategoryService categoryService, ProductService productService,
                   OrderService orderService, AppUserService appUserService,
                   TelegramFileService telegramFileService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.orderService = orderService;
        this.appUserService = appUserService;
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
            log.error("Error processing update", e);
        }
    }

    private void handleMessage(Message message) throws TelegramApiException {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText();

        // Create or update user
        appUserService.createOrUpdateUser(
                userId,
                message.getFrom().getUserName(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName()
        );

        UserSession session = userSessions.computeIfAbsent(userId, k -> new UserSession());

        if (text != null) {
            switch (text) {
                case "/start":
                    sendWelcomeMessage(chatId);
                    break;
                case "🛍️ Browse Products":
                    showCategories(chatId);
                    break;
                case "🛒 View Cart":
                    showCart(chatId, userId);
                    break;
                case "📋 My Orders":
                    showUserOrders(chatId, userId);
                    break;
                case "ℹ️ Help":
                    sendHelpMessage(chatId);
                    break;
                default:
                    handleUserInput(chatId, userId, text, session);
                    break;
            }
        } else if (message.hasPhoto()) {
            handlePhotoUpload(chatId, userId, message, session);
        }else if (message.hasLocation()) {
            handleLocationInput(chatId, userId, message.getLocation(), session);
        }

    }
    private void handleLocationInput(Long chatId, Long userId,
                                     org.telegram.telegrambots.meta.api.objects.Location location,
                                     UserSession session) throws TelegramApiException {
        if (session.getCheckoutStep() == UserSession.CheckoutStep.DELIVERY_LOCATION) {
            // Always enforce dot decimal separator
            String mapsLink = String.format(
                    java.util.Locale.US,
                    "https://maps.google.com/?q=%f,%f",
                    location.getLatitude(),
                    location.getLongitude()
            );

            // Save for order summary
            session.setDeliveryAddress(mapsLink);

            processOrder(chatId, userId, session);

            // Send confirmation to user
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText("✅ Location received!\n\n" +
                    "📍 [Open Location](" + mapsLink.replace(")", "\\)") + ")");
            msg.setParseMode("MarkdownV2"); // ✅ safer than plain Markdown
            msg.setReplyMarkup(createMainKeyboard());
            execute(msg);
        }
    }





    private void handleCallbackQuery(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        String[] parts = callbackData.split(":");
        String action = parts[0];

        switch (action) {
            case "category":
                Long categoryId = Long.parseLong(parts[1]);
                showProductsInCategory(chatId, categoryId);
                break;
            case "product":
                Long productId = Long.parseLong(parts[1]);
                showProductDetails(chatId, productId);
                break;
            case "add_to_cart":
                Long addProductId = Long.parseLong(parts[1]);
                addToCart(chatId, userId, addProductId);
                break;
            case "remove_from_cart":
                Long removeProductId = Long.parseLong(parts[1]);
                removeFromCart(chatId, userId, removeProductId);
                break;
            case "checkout":
                startCheckout(chatId, userId);
                break;
            case "back_to_categories":
                showCategories(chatId);
                break;
            case "back_to_products":
                Long backCategoryId = Long.parseLong(parts[1]);
                showProductsInCategory(chatId, backCategoryId);
                break;
        }
    }

    private void sendWelcomeMessage(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Welcome to Wristwatch Shop! 🕰️\n\n" +
                "Browse our collection of premium watches and find your perfect timepiece.\n\n" +
                "Use the menu below to get started:");
        message.setReplyMarkup(createMainKeyboard());
        execute(message);
    }

    private void sendHelpMessage(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("How to use Wristwatch Shop Bot:\n\n" +
                "🛍️ Browse Products - View our watch categories\n" +
                "🛒 View Cart - See items in your shopping cart\n" +
                "📋 My Orders - Check your order history\n\n" +
                "To place an order:\n" +
                "1. Browse and add items to cart\n" +
                "2. Go to cart and checkout\n" +
                "3. Provide delivery details\n" +
                "4. Make payment and upload screenshot\n" +
                "5. Wait for payment verification\n\n" +
                "Need help? Contact our support team!");
        execute(message);
    }

    private void showCategories(Long chatId) throws TelegramApiException {
        List<CategoryDto> categories = categoryService.getAllCategories();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a category to browse:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (CategoryDto category : categories) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category.getName());
            button.setCallbackData("category:" + category.getId());
            rows.add(Arrays.asList(button));
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showProductsInCategory(Long chatId, Long categoryId) throws TelegramApiException {
        List<ProductDto> products = productService.getAvailableProductsByCategory(categoryId);
        CategoryDto category = categoryService.getCategoryById(categoryId);

        if (products.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No products available in " + category.getName() + " category.");
            execute(message);
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Products in " + category.getName() + ":");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ProductDto product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() + " - $" + product.getPrice());
            button.setCallbackData("product:" + product.getId());
            rows.add(Arrays.asList(button));
        }

        // Back button
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Categories");
        backButton.setCallbackData("back_to_categories");
        rows.add(Arrays.asList(backButton));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void showProductDetails(Long chatId, Long productId) throws TelegramApiException {
        ProductDto product = productService.getProductById(productId);

        String productText = "🕰️ " + product.getName() + "\n\n" +
                "💰 Price: $" + product.getPrice() + "\n" +
                "📦 Stock: " + product.getStock() + " available\n" +
                "📂 Category: " + product.getCategoryName() + "\n\n" +
                "📝 Description:\n" + (product.getDescription() != null ? product.getDescription() : "No description available");

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            // Send photo with caption
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(new File(product.getImageUrl())));
            photo.setCaption(productText);

            InlineKeyboardMarkup keyboard = createProductKeyboard(productId, product.getCategoryId());
            photo.setReplyMarkup(keyboard);

            try {
                execute(photo);
            } catch (TelegramApiException e) {
                // If photo fails, send text message
                sendProductTextMessage(chatId, productText, productId, product.getCategoryId());
            }
        } else {
            sendProductTextMessage(chatId, productText, productId, product.getCategoryId());
        }
    }

    private void sendProductTextMessage(Long chatId, String text, Long productId, Long categoryId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(createProductKeyboard(productId, categoryId));
        execute(message);
    }

    private InlineKeyboardMarkup createProductKeyboard(Long productId, Long categoryId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Add to cart button
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("🛒 Add to Cart");
        addButton.setCallbackData("add_to_cart:" + productId);
        rows.add(Arrays.asList(addButton));

        // Back button
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Products");
        backButton.setCallbackData("back_to_products:" + categoryId);
        rows.add(Arrays.asList(backButton));

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void addToCart(Long chatId, Long userId, Long productId) throws TelegramApiException {
        UserSession session = userSessions.computeIfAbsent(userId, k -> new UserSession());

        ProductDto product = productService.getProductById(productId);

        // Check if product already in cart
        CartItemDto existingItem = session.getCart().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            if (existingItem.getQuantity() < product.getStock()) {
                existingItem.setQuantity(existingItem.getQuantity() + 1);
                existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            } else {
                sendMessage(chatId, "Cannot add more items. Stock limit reached!");
                return;
            }
        } else {
            CartItemDto cartItem = new CartItemDto();
            cartItem.setProductId(productId);
            cartItem.setProductName(product.getName());
            cartItem.setUnitPrice(product.getPrice());
            cartItem.setQuantity(1);
            cartItem.setTotalPrice(product.getPrice());
            cartItem.setAvailableStock(product.getStock());
            session.getCart().add(cartItem);
        }

        sendMessage(chatId, "✅ " + product.getName() + " added to cart!");
    }

    private void removeFromCart(Long chatId, Long userId, Long productId) throws TelegramApiException {
        UserSession session = userSessions.get(userId);
        if (session != null) {
            session.getCart().removeIf(item -> item.getProductId().equals(productId));
            sendMessage(chatId, "❌ Item removed from cart!");
        }
    }

    private void showCart(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = userSessions.get(userId);
        if (session == null || session.getCart().isEmpty()) {
            sendMessage(chatId, "🛒 Your cart is empty!\n\nUse 'Browse Products' to add items.");
            return;
        }

        StringBuilder cartText = new StringBuilder("🛒 Your Cart:\n\n");
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDto item : session.getCart()) {
            cartText.append("• ").append(item.getProductName()).append("\n");
            cartText.append("  Quantity: ").append(item.getQuantity()).append("\n");
            cartText.append("  Price: $").append(item.getUnitPrice()).append(" each\n");
            cartText.append("  Subtotal: $").append(item.getTotalPrice()).append("\n\n");
            total = total.add(item.getTotalPrice());
        }

        cartText.append("💰 Total: $").append(total);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(cartText.toString());
        message.setReplyMarkup(createCartKeyboard(session.getCart()));
        execute(message);
    }

    private InlineKeyboardMarkup createCartKeyboard(List<CartItemDto> cartItems) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Remove buttons for each item
        for (CartItemDto item : cartItems) {
            InlineKeyboardButton removeButton = new InlineKeyboardButton();
            removeButton.setText("❌ Remove " + item.getProductName());
            removeButton.setCallbackData("remove_from_cart:" + item.getProductId());
            rows.add(Arrays.asList(removeButton));
        }

        // Checkout button
        if (!cartItems.isEmpty()) {
            InlineKeyboardButton checkoutButton = new InlineKeyboardButton();
            checkoutButton.setText("💳 Checkout");
            checkoutButton.setCallbackData("checkout");
            rows.add(Arrays.asList(checkoutButton));
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void startCheckout(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = userSessions.get(userId);
        if (session == null || session.getCart().isEmpty()) {
            sendMessage(chatId, "Your cart is empty!");
            return;
        }

        session.setCheckoutStep(CheckoutStep.CUSTOMER_NAME);
        sendMessage(chatId, "🛍️ Checkout Process Started!\n\nPlease enter your full name:");
    }

    private void handleUserInput(Long chatId, Long userId, String text, UserSession session) throws TelegramApiException {
        if (session.getCheckoutStep() != null) {
            handleCheckoutInput(chatId, userId, text, session);
        } else {
            sendMessage(chatId, "I don't understand that command. Use the menu buttons or type /start to begin.");
        }
    }

    private void handleCheckoutInput(Long chatId, Long userId, String text, UserSession session) throws TelegramApiException {
        switch (session.getCheckoutStep()) {
            case CUSTOMER_NAME:
                session.setCustomerName(text);
                session.setCheckoutStep(CheckoutStep.CUSTOMER_PHONE);
                sendMessage(chatId, "📱 Please enter your phone number:");
                break;

            case CUSTOMER_PHONE:
                session.setCustomerPhone(text);
                session.setCheckoutStep(CheckoutStep.DELIVERY_LOCATION);

                SendMessage locationRequest = new SendMessage();
                locationRequest.setChatId(chatId);
                locationRequest.setText("📍 Please share your delivery location:");

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);

                KeyboardRow row = new KeyboardRow();
                KeyboardButton locationButton = new KeyboardButton("📍 Share Location");
                locationButton.setRequestLocation(true);
                row.add(locationButton);

                keyboard.setKeyboard(Collections.singletonList(row));
                locationRequest.setReplyMarkup(keyboard);

                execute(locationRequest);
                break;

            // ❌ remove DELIVERY_ADDRESS here, location will be handled by handleLocationInput()
        }
    }

    private void processOrder(Long chatId, Long userId, UserSession session) throws TelegramApiException {
        try {
            OrderCreateRequest request = new OrderCreateRequest();
            request.setTelegramId(userId);
            request.setCustomerName(session.getCustomerName());
            request.setCustomerPhone(session.getCustomerPhone());
            request.setDeliveryAddress(session.getDeliveryAddress());
            request.setCartItems(session.getCart());

            OrderDto order = orderService.createOrder(request);

            // Clear cart and checkout state
            session.getCart().clear();
            session.setCheckoutStep(null);
            session.setPendingOrderId(order.getId());
            session.setAwaitingPaymentProof(true);

            String orderSummary = "✅ Order Created Successfully!\n\n" +
                    "📋 Order #" + order.getId() + "\n" +
                    "👤 Customer: " + order.getCustomerName() + "\n" +
                    "📱 Phone: " + order.getCustomerPhone() + "\n" +
                    "📍 Address: " + order.getDeliveryAddress() + "\n" +
                    "💰 Total: $" + order.getTotalAmount() + "\n\n" +
                    "💳 Payment Details:\n" + merchantDetails + "\n\n" +
                    "📸 Please make the payment and upload a screenshot as proof.";

            sendMessage(chatId, orderSummary);

        } catch (Exception e) {
            sendMessage(chatId, "❌ Error creating order: " + e.getMessage());
            session.setCheckoutStep(null);
        }
    }

    private void handlePhotoUpload(Long chatId, Long userId, Message message, UserSession session) throws TelegramApiException {
        if (!session.isAwaitingPaymentProof()) {
            sendMessage(chatId, "I'm not expecting a photo right now. Please use the menu to navigate.");
            return;
        }

        try {
            String filePath = telegramFileService.downloadAndStorePhoto(message, this, "payment-proofs");

            Long orderId = session.getPendingOrderId();
            String fileName = "payment_proof_" + orderId + "_" + System.currentTimeMillis() + ".jpg";

            orderService.uploadPaymentProof(orderId, filePath, fileName);

            session.setAwaitingPaymentProof(false);
            session.setPendingOrderId(null);

            sendMessage(chatId, "✅ Payment proof uploaded successfully!\n\n" +
                    "Your order is now awaiting verification. You will be notified once the payment is approved.\n\n" +
                    "Thank you for shopping with us! 🕰️");

        } catch (Exception e) {
            log.error("Error uploading payment proof", e);
            sendMessage(chatId, "❌ Error uploading payment proof: " + e.getMessage());
        }
    }

    private void showUserOrders(Long chatId, Long userId) throws TelegramApiException {
        List<OrderDto> orders = orderService.getOrdersByUser(userId);

        if (orders.isEmpty()) {
            sendMessage(chatId, "📋 You have no orders yet.\n\nStart shopping to place your first order!");
            return;
        }

        StringBuilder ordersText = new StringBuilder("📋 Your Orders:\n\n");

        for (OrderDto order : orders) {
            ordersText.append("🆔 Order #").append(order.getId()).append("\n");
            ordersText.append("📅 Date: ").append(order.getCreatedAt().toLocalDate()).append("\n");
            ordersText.append("💰 Total: $").append(order.getTotalAmount()).append("\n");
            ordersText.append("📊 Status: ").append(getStatusEmoji(order.getStatus())).append(" ").append(order.getStatus()).append("\n\n");
        }

        sendMessage(chatId, ordersText.toString());
    }

    private String getStatusEmoji(Order.OrderStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case AWAITING_VERIFICATION: return "🔍";
            case PAID: return "✅";
            case REJECTED: return "❌";
            case CANCELLED: return "🚫";
            default: return "❓";
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🛍️ Browse Products"));
        row1.add(new KeyboardButton("🛒 View Cart"));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📋 My Orders"));
        row2.add(new KeyboardButton("ℹ️ Help"));
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }

    // Notification method for payment verification results
    public void notifyPaymentResult(Long userId, Long orderId, boolean approved) {
        try {
            String message = approved ?
                    "✅ Payment Approved!\n\nYour order #" + orderId + " has been confirmed. We'll process it shortly!" :
                    "❌ Payment Rejected\n\nYour payment for order #" + orderId + " was not approved. Please contact support for assistance.";

            sendMessage(userId, message);
        } catch (TelegramApiException e) {
            log.error("Failed to send payment notification to user " + userId, e);
        }
    }

    // Event listener for payment verification notifications
    @EventListener
    public void handlePaymentVerificationEvent(PaymentVerificationEvent event) {
        notifyPaymentResult(event.getUserId(), event.getOrderId(), event.isApproved());
    }
}
