package com.wristwatch.shop.service;

import com.wristwatch.shop.event.PaymentVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {


    @EventListener
    public void handlePaymentVerification(PaymentVerificationEvent event) {
        try {
            // We'll get the UserBot through ApplicationContext when needed
            notifyPaymentVerification(event.getUserId(), event.getOrderId(), event.isApproved());
        } catch (Exception e) {
            log.error("Failed to send payment notification", e);
        }
    }

    private void notifyPaymentVerification(Long userId, Long orderId, boolean approved) {
        log.info("Payment verification notification: userId={}, orderId={}, approved={}", userId, orderId, approved);
    }
}
