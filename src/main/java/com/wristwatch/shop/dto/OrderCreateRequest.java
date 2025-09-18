package com.wristwatch.shop.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class OrderCreateRequest {
    
    @NotNull(message = "Telegram ID is required")
    private Long telegramId;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;
    
    @NotBlank(message = "Customer phone is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String customerPhone;
    
    @NotBlank(message = "Delivery address is required")
    @Size(max = 1000, message = "Delivery address must not exceed 1000 characters")
    private String deliveryAddress;
    
    @NotEmpty(message = "Cart items are required")
    @Valid
    private List<CartItemDto> cartItems;
}
