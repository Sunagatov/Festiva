package com.festiva.i18n;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public abstract class MessagesTestSupport {

    @BeforeAll
    static void initMessages() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        Messages.initForTest(source);
    }
}
