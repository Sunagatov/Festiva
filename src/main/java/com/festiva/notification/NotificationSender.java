package com.festiva.notification;

public interface NotificationSender {

    void send(long telegramUserId, String text);
}
