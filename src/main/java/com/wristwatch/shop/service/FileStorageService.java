package com.wristwatch.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;
    
    public String storePaymentProof(MultipartFile file) throws IOException {
        return storeFile(file, "payment-proofs");
    }
    
    public String storeProductImage(MultipartFile file) throws IOException {
        return storeFile(file, "products");
    }
    
    private String storeFile(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }
        
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, subDirectory);
        Files.createDirectories(uploadPath);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        // Store file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Stored file: {}", filePath.toString());
        return filePath.toString();
    }
    
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }
    
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
