package com.wristwatch.shop.service;

import com.wristwatch.shop.dto.*;
import com.wristwatch.shop.entity.*;
import com.wristwatch.shop.event.PaymentProofUploadedEvent;
import com.wristwatch.shop.event.PaymentVerificationEvent;
import com.wristwatch.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final AppUserRepository appUserRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByUser(Long telegramId) {
        return orderRepository.findByUserTelegramIdOrderByCreatedAtDesc(telegramId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        return convertToDto(order);
    }

    public OrderDto createOrder(OrderCreateRequest request) {
        // Find or create user
        AppUser user = appUserRepository.findByTelegramId(request.getTelegramId())
                .orElseThrow(() -> new RuntimeException("User not found with telegram ID: " + request.getTelegramId()));

        // Validate cart items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemDto cartItem : request.getCartItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + cartItem.getProductId()));

            if (!product.getIsActive()) {
                throw new RuntimeException("Product is not available: " + product.getName());
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        // Create order items
        for (CartItemDto cartItem : request.getCartItems()) {
            Product product = productRepository.findById(cartItem.getProductId()).get();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItemRepository.save(orderItem);
        }

        return convertToDto(savedOrder);
    }

    public void updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        orderRepository.save(order);

        // If order is approved (PAID), reduce product stock
        if (status == Order.OrderStatus.PAID && oldStatus != Order.OrderStatus.PAID) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : orderItems) {
                productService.reduceStock(item.getProduct().getId(), item.getQuantity());
            }
        }
    }

    public void uploadPaymentProof(Long orderId, String filePath, String fileName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot upload payment proof for order with status: " + order.getStatus());
        }

        PaymentProof paymentProof = new PaymentProof();
        paymentProof.setOrder(order);
        paymentProof.setFilePath(filePath);
        paymentProof.setFileName(fileName);

        paymentProofRepository.save(paymentProof);

        // Update order status to awaiting verification
        order.setStatus(Order.OrderStatus.AWAITING_VERIFICATION);
        orderRepository.save(order);

        eventPublisher.publishEvent(new PaymentProofUploadedEvent(orderId, order.getUser().getTelegramId()));
    }

    @Transactional(readOnly = true)
    public String getPaymentProofPath(Long orderId) {
        return paymentProofRepository.findByOrderId(orderId)
                .stream()
                .findFirst()
                .map(PaymentProof::getFilePath)
                .orElse(null);
    }

    public void verifyPayment(Long orderId, boolean approved, Long adminTelegramId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        AppUser admin = appUserRepository.findByTelegramId(adminTelegramId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!admin.getIsAdmin()) {
            throw new RuntimeException("User is not authorized to verify payments");
        }

        PaymentProof paymentProof = paymentProofRepository.findByOrderIdAndVerifiedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("No unverified payment proof found for order"));

        paymentProof.setVerifiedAt(LocalDateTime.now());
        paymentProof.setVerifiedBy(admin);
        paymentProofRepository.save(paymentProof);

        if (approved) {
            updateOrderStatus(orderId, Order.OrderStatus.PAID);
        } else {
            updateOrderStatus(orderId, Order.OrderStatus.REJECTED);
        }

        eventPublisher.publishEvent(new PaymentVerificationEvent(order.getUser().getTelegramId(), orderId, approved));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getPendingVerificationOrders() {
        return getOrdersByStatus(Order.OrderStatus.AWAITING_VERIFICATION);
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Load order items
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        dto.setOrderItems(orderItems.stream().map(this::convertOrderItemToDto).collect(Collectors.toList()));

        // Check if has payment proof
        dto.setHasPaymentProof(!paymentProofRepository.findByOrderId(order.getId()).isEmpty());

        return dto;
    }

    private OrderItemDto convertOrderItemToDto(OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getId());
        dto.setProductId(orderItem.getProduct().getId());
        dto.setProductName(orderItem.getProduct().getName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setUnitPrice(orderItem.getUnitPrice());
        dto.setTotalPrice(orderItem.getTotalPrice());
        return dto;
    }
}
