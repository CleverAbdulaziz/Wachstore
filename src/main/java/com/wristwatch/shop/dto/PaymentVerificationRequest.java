package com.wristwatch.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class PaymentVerificationRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Approval status is required")
    private Boolean approved;
    
    private String notes;
}
