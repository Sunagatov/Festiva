package com.festiva.i18n;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.support.ResourceBundleMessageSource;

public abstract class MessagesTestSupport {

    @BeforeAll
    static void initMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        Messages.initForTest(source);
    }
}
