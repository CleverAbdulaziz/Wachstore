package com.wristwatch.shop.repository;

import com.wristwatch.shop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);
    
    List<Product> findByIsActiveTrueOrderByNameAsc();
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0 ORDER BY p.name ASC")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true AND p.stock > 0 ORDER BY p.name ASC")
    List<Product> findAvailableProductsByCategory(@Param("categoryId") Long categoryId);
    
    boolean existsByName(String name);
}
