package com.wristwatch.shop.bot;

import com.wristwatch.shop.dto.CartItemDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {

    public static CheckoutStep CheckoutStep;
    private List<CartItemDto> cart = new ArrayList<>();
    private com.wristwatch.shop.bot.CheckoutStep checkoutStep;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private boolean awaitingPaymentProof = false;
    private Long pendingOrderId;

    public void setCheckoutStep(com.wristwatch.shop.bot.CheckoutStep checkoutStep) {
        this.checkoutStep = checkoutStep;
    }





}
