package com.wristwatch.shop.service;

import com.wristwatch.shop.entity.AppUser;
import com.wristwatch.shop.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

    private final AppUserRepository appUserRepository;

    @Value("${admin.telegram.ids:}")
    private String adminTelegramIds;

    @Transactional(readOnly = true)
    public Optional<AppUser> findByTelegramId(Long telegramId) {
        return appUserRepository.findByTelegramId(telegramId);
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(Long telegramId) {
        // First check database
        boolean isDbAdmin = appUserRepository.existsByTelegramIdAndIsAdminTrue(telegramId);

        // Also check configuration-based admin IDs
        boolean isConfigAdmin = false;
        if (adminTelegramIds != null && !adminTelegramIds.trim().isEmpty()) {
            List<String> adminIds = Arrays.asList(adminTelegramIds.split(","));
            isConfigAdmin = adminIds.contains(telegramId.toString());
        }

        return isDbAdmin || isConfigAdmin;
    }

    @Transactional(readOnly = true)
    public Long getFirstAdminTelegramId() {
        // First try to get from database
        Optional<AppUser> dbAdmin = appUserRepository.findFirstByIsAdminTrue();
        if (dbAdmin.isPresent()) {
            return dbAdmin.get().getTelegramId();
        }

        // Fallback to configuration-based admin IDs
        if (adminTelegramIds != null && !adminTelegramIds.trim().isEmpty()) {
            String[] adminIds = adminTelegramIds.split(",");
            if (adminIds.length > 0) {
                try {
                    return Long.parseLong(adminIds[0].trim());
                } catch (NumberFormatException e) {
                    // Invalid format, continue to next
                }
            }
        }

        return null;
    }

    public AppUser createOrUpdateUser(Long telegramId, String username, String firstName, String lastName) {
        Optional<AppUser> existingUser = appUserRepository.findByTelegramId(telegramId);

        if (existingUser.isPresent()) {
            AppUser user = existingUser.get();
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            if (isConfigAdmin(telegramId) && !user.getIsAdmin()) {
                user.setIsAdmin(true);
            }
            return appUserRepository.save(user);
        } else {
            AppUser newUser = new AppUser();
            newUser.setTelegramId(telegramId);
            newUser.setUsername(username);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setIsAdmin(isConfigAdmin(telegramId));
            return appUserRepository.save(newUser);
        }
    }

    private boolean isConfigAdmin(Long telegramId) {
        if (adminTelegramIds != null && !adminTelegramIds.trim().isEmpty()) {
            List<String> adminIds = Arrays.asList(adminTelegramIds.split(","));
            return adminIds.contains(telegramId.toString());
        }
        return false;
    }
}
