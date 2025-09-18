package com.wristwatch.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CartItemDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private String productName;
    
    private BigDecimal unitPrice;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private BigDecimal totalPrice;
    
    private Integer availableStock;
}
