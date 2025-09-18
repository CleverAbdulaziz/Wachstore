package com.wristwatch.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WristwatchShopApplication {
    public static void main(String[] args) {
        try {
            System.out.println("=== Bot is starting (before Spring) ===");
            SpringApplication.run(WristwatchShopApplication.class, args);
            System.out.println("=== Bot started successfully (after Spring) ===");
        } catch (Exception e) {
            e.printStackTrace(); // ensures you see crash in Log Stream
        }    }
}
