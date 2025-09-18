package com.wristwatch.shop.repository;

import com.wristwatch.shop.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    boolean existsByTelegramIdAndIsAdminTrue(Long telegramId);

    Optional<AppUser> findFirstByIsAdminTrue();
}
