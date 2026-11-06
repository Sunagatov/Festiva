package com.festiva.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import okhttp3.OkHttpClient;
import java.time.Duration;

@Configuration
@SuppressWarnings("unused")
public class TelegramClientConfig {

    @Bean
    public TelegramClient telegramClient(@Value("${telegram.bot.token}") String token) {
        return new OkHttpTelegramClient(new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .callTimeout(Duration.ofSeconds(10))
                .build(), token);
    }
}
