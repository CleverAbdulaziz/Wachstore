package com.wristwatch.shop.service;

import com.wristwatch.shop.dto.ProductCreateRequest;
import com.wristwatch.shop.dto.ProductDto;
import com.wristwatch.shop.entity.Category;
import com.wristwatch.shop.entity.Product;
import com.wristwatch.shop.repository.CategoryRepository;
import com.wristwatch.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getAvailableProducts() {
        return productRepository.findAvailableProducts()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getAvailableProductsByCategory(Long categoryId) {
        return productRepository.findAvailableProductsByCategory(categoryId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDto(product);
    }
    
    public ProductDto createProduct(ProductCreateRequest request) {
        if (productRepository.existsByName(request.getName())) {
            throw new RuntimeException("Product with name '" + request.getName() + "' already exists");
        }
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
        
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        product.setIsActive(true);
        
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }
    
    public ProductDto updateProduct(Long id, ProductCreateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Check if name is being changed and if new name already exists
        if (!product.getName().equals(request.getName()) && 
            productRepository.existsByName(request.getName())) {
            throw new RuntimeException("Product with name '" + request.getName() + "' already exists");
        }
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }
    
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    public void updateStock(Long productId, Integer newStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        product.setStock(newStock);
        productRepository.save(product);
    }
    
    public boolean reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        if (product.getStock() < quantity) {
            return false; // Insufficient stock
        }
        
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
        return true;
    }
    
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setImageUrl(product.getImageUrl());
        dto.setIsActive(product.getIsActive());
        return dto;
    }
}
