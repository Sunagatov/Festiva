package com.festiva.i18n;

import java.util.Locale;

public enum Lang {
    EN, RU;

    private static final Locale RUSSIAN = Locale.of("ru");

    public Locale locale() {
        return this == EN ? Locale.ENGLISH : RUSSIAN;
    }
}
