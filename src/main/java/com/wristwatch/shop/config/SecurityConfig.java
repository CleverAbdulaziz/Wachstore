package com.wristwatch.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/orders/**").permitAll()
                .requestMatchers("/api/payment-proofs/file/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll() // In production, add proper authentication
                .requestMatchers("/api/statistics/**").permitAll() // In production, add proper authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
