package com.wristwatch.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
