package com.festiva.notification;

import org.telegram.telegrambots.meta.api.objects.InputFile;

public interface NotificationSender {

    void send(long telegramUserId, String text);

    void sendDocument(long chatId, InputFile file, String caption);
}
