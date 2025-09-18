package com.wristwatch.shop.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentProofUploadedEvent {
    private Long orderId;
    private Long customerTelegramId;
}
