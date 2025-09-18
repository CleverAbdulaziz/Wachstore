package com.wristwatch.shop.config;

import com.wristwatch.shop.bot.AdminBot;
import com.wristwatch.shop.bot.UserBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final UserBot userBot;
    private final AdminBot adminBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Make sure each bot has its own valid token
        botsApi.registerBot(userBot);
        botsApi.registerBot(adminBot);

        return botsApi;
    }
}
