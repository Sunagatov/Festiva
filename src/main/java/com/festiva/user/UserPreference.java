package com.festiva.user;

import com.festiva.i18n.Lang;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user_preferences")
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    public static final String DEFAULT_TIMEZONE = "Europe/Moscow";

    @Id
    private long telegramUserId;
    private Lang lang;
    private int notifyHour = -1;
    private String timezone = UserPreference.DEFAULT_TIMEZONE;
}
