package com.wristwatch.shop.controller;

import com.wristwatch.shop.dto.OrderDto;
import com.wristwatch.shop.dto.PaymentVerificationRequest;
import com.wristwatch.shop.entity.Order;
import com.wristwatch.shop.service.OrderService;
import com.wristwatch.shop.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {
    
    private final OrderService orderService;
    private final StatisticsService statisticsService;
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getPendingOrders(@RequestParam(required = false) String status) {
        try {
            List<OrderDto> orders;
            if (status != null) {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orders = orderService.getOrdersByStatus(orderStatus);
            } else {
                orders = orderService.getPendingVerificationOrders();
            }
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        try {
            OrderDto order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/orders/{id}/verify")
    public ResponseEntity<String> verifyPayment(@PathVariable Long id, 
                                               @Valid @RequestBody PaymentVerificationRequest request,
                                               @RequestParam Long adminTelegramId) {
        try {
            orderService.verifyPayment(id, request.getApproved(), adminTelegramId);
            String message = request.getApproved() ? "Payment approved" : "Payment rejected";
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/stats/sales")
    public ResponseEntity<Map<String, Object>> getSalesStats(@RequestParam(defaultValue = "daily") String period) {
        Map<String, Object> stats;
        switch (period.toLowerCase()) {
            case "weekly":
                stats = statisticsService.getWeeklySales();
                break;
            case "monthly":
                stats = statisticsService.getMonthlySales();
                break;
            default:
                stats = statisticsService.getDailySales();
                break;
        }
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats/top-products")
    public ResponseEntity<List<Object[]>> getTopProducts(@RequestParam(defaultValue = "30") int days) {
        List<Object[]> topProducts = statisticsService.getTopSellingProducts(days);
        return ResponseEntity.ok(topProducts);
    }
}
