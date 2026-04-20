package com.festiva.notification;

public interface NotificationSender {

    boolean send(long telegramUserId, String text);
}
