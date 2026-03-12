package com.festiva.i18n;

import java.util.Locale;

public enum Lang {
    EN("en"), RU("ru");

    private static final Locale RUSSIAN = Locale.of("ru");
    private final String code;

    Lang(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public Locale locale() {
        return this == EN ? Locale.ENGLISH : RUSSIAN;
    }
}
