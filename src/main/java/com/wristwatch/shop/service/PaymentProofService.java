package com.wristwatch.shop.service;

import com.wristwatch.shop.dto.PaymentProofDto;
import com.wristwatch.shop.entity.PaymentProof;
import com.wristwatch.shop.repository.PaymentProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentProofService {
    
    private final PaymentProofRepository paymentProofRepository;
    
    public List<PaymentProofDto> getPaymentProofsByOrder(Long orderId) {
        return paymentProofRepository.findByOrderId(orderId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<PaymentProofDto> getPendingVerificationProofs() {
        return paymentProofRepository.findByVerifiedAtIsNullOrderByUploadedAtAsc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private PaymentProofDto convertToDto(PaymentProof paymentProof) {
        PaymentProofDto dto = new PaymentProofDto();
        dto.setId(paymentProof.getId());
        dto.setOrderId(paymentProof.getOrder().getId());
        dto.setFileName(paymentProof.getFileName());
        dto.setFilePath(paymentProof.getFilePath());
        dto.setUploadedAt(paymentProof.getUploadedAt());
        dto.setVerifiedAt(paymentProof.getVerifiedAt());
        dto.setVerified(paymentProof.getVerifiedAt() != null);
        
        if (paymentProof.getVerifiedBy() != null) {
            dto.setVerifiedByName(paymentProof.getVerifiedBy().getFirstName() + " " + 
                                 paymentProof.getVerifiedBy().getLastName());
        }
        
        return dto;
    }
}
