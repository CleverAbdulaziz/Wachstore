package com.wristwatch.shop.service;

import com.wristwatch.shop.dto.CategoryDto;
import com.wristwatch.shop.entity.Category;
import com.wristwatch.shop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllOrderByName()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return convertToDto(category);
    }
    
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }
    
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        // Check if name is being changed and if new name already exists
        if (!category.getName().equals(categoryDto.getName()) && 
            categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }
    
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
    
    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }
}
