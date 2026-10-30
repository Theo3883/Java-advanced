package org.example.vros.service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Utility service to retrieve localized messages from Resource Bundles
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String getMessage(String code, Object... args) {
        return getMessage(code, LocaleContextHolder.getLocale(), args);
    }

    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
