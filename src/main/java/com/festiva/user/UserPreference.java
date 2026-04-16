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

    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final Lang DEFAULT_LANG = Lang.EN;

    @Id
    private long telegramUserId;
    private Lang lang = DEFAULT_LANG;
    private int notifyHour = 9;
    private String timezone = DEFAULT_TIMEZONE;
    private java.time.LocalDate lastNotifiedDate;
}
