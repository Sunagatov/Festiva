package com.festiva.user.api;

import com.festiva.i18n.Lang;

public final class UserLanguageAction {

    public static final String PREFIX = "LANG_";

    private UserLanguageAction() {
    }

    public static String callback(Lang lang) {
        return PREFIX + lang.name();
    }
}
