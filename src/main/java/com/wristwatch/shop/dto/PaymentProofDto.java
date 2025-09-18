package com.wristwatch.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentProofDto {
    
    private Long id;
    private Long orderId;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private boolean isVerified;
}
