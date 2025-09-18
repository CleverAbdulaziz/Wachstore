package com.wristwatch.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WristwatchShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(WristwatchShopApplication.class, args);
    }
}
