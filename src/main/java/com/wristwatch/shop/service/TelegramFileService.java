package com.wristwatch.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TelegramFileService {

    @Autowired
    private FileStorageService fileStorageService;

    public String downloadAndStorePhoto(Message message, TelegramLongPollingBot bot, String subDirectory) throws TelegramApiException, IOException {
        if (!message.hasPhoto()) {
            throw new IllegalArgumentException("Message does not contain a photo");
        }

        // Get the largest photo
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize photo = photos.get(photos.size() - 1);

        // Get file info from Telegram
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(photo.getFileId());
        File file = bot.execute(getFileMethod);

        // Download file from Telegram servers
        String fileUrl = "https://api.telegram.org/file/bot" + bot.getBotToken() + "/" + file.getFilePath();

        // Generate unique filename
        String extension = getFileExtension(file.getFilePath());
        String filename = UUID.randomUUID().toString() + extension;

        // Create directory if it doesn't exist
        String uploadDir = "./uploads";
        Path uploadPath = Paths.get(uploadDir, subDirectory);
        Files.createDirectories(uploadPath);

        // Download and save file
        Path filePath = uploadPath.resolve(filename);
        try (InputStream inputStream = new URL(fileUrl).openStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Downloaded and stored Telegram photo: {}", filePath.toString());
        return filePath.toString();
    }

    private String getFileExtension(String filePath) {
        if (filePath != null && filePath.contains(".")) {
            return filePath.substring(filePath.lastIndexOf("."));
        }
        return ".jpg"; // Default extension
    }
}
