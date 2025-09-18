package com.wristwatch.shop.controller;

import com.wristwatch.shop.dto.OrderCreateRequest;
import com.wristwatch.shop.dto.OrderDto;
import com.wristwatch.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        try {
            OrderDto createdOrder = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        try {
            OrderDto order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{telegramId}")
    public ResponseEntity<List<OrderDto>> getOrdersByUser(@PathVariable Long telegramId) {
        List<OrderDto> orders = orderService.getOrdersByUser(telegramId);
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping("/{id}/payment-proof")
    public ResponseEntity<String> uploadPaymentProof(@PathVariable Long id, 
                                                    @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            
            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("uploads/payment-proofs");
            Files.createDirectories(uploadDir);
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // Save to database
            orderService.uploadPaymentProof(id, filePath.toString(), filename);
            
            return ResponseEntity.ok("Payment proof uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
