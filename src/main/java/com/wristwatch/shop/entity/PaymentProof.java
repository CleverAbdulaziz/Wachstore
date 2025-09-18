package com.wristwatch.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_proof")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class PaymentProof {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @CreatedDate
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private AppUser verifiedBy;
}
