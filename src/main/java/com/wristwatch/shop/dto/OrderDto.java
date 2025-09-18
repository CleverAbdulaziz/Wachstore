package com.wristwatch.shop.dto;

import com.wristwatch.shop.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    
    private Long id;
    private Long userId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> orderItems;
    private boolean hasPaymentProof;
}
