package com.wristwatch.shop.repository;

import com.wristwatch.shop.entity.PaymentProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProofRepository extends JpaRepository<PaymentProof, Long> {
    
    List<PaymentProof> findByOrderId(Long orderId);
    
    Optional<PaymentProof> findByOrderIdAndVerifiedAtIsNull(Long orderId);
    
    List<PaymentProof> findByVerifiedAtIsNullOrderByUploadedAtAsc();
}
