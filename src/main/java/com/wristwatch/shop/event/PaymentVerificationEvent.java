package com.wristwatch.shop.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentVerificationEvent {
    private Long userId;
    private Long orderId;
    private boolean approved;
}
