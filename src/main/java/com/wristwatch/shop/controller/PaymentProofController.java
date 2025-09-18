package com.wristwatch.shop.controller;

import com.wristwatch.shop.dto.PaymentProofDto;
import com.wristwatch.shop.service.PaymentProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/payment-proofs")
@RequiredArgsConstructor
public class PaymentProofController {
    
    private final PaymentProofService paymentProofService;
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentProofDto>> getPaymentProofsByOrder(@PathVariable Long orderId) {
        List<PaymentProofDto> proofs = paymentProofService.getPaymentProofsByOrder(orderId);
        return ResponseEntity.ok(proofs);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<PaymentProofDto>> getPendingVerificationProofs() {
        List<PaymentProofDto> proofs = paymentProofService.getPendingVerificationProofs();
        return ResponseEntity.ok(proofs);
    }
    
    @GetMapping("/file/{filename:.+}")
    public ResponseEntity<Resource> getPaymentProofFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/payment-proofs").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
