package com.festiva.i18n;

import java.util.Locale;

public enum Lang {
    EN, RU;

    public Locale locale() {
        return this == EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }
}
