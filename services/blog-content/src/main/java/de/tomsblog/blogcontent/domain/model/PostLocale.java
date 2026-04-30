package de.tomsblog.blogcontent.domain.model;

import java.util.Locale;
import java.util.Objects;

public record PostLocale(Locale locale) {

    public PostLocale {
        Objects.requireNonNull(locale, "Locale must not be null");
    }

    public static PostLocale of(String languageTag) {
        return new PostLocale(Locale.forLanguageTag(languageTag));
    }

    public static PostLocale german() {
        return new PostLocale(Locale.GERMAN);
    }

    public static PostLocale english() {
        return new PostLocale(Locale.ENGLISH);
    }

    public String languageTag() {
        return locale.toLanguageTag();
    }
}
