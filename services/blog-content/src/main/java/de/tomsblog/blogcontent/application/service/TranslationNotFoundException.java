package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.domain.model.TranslationId;

public class TranslationNotFoundException extends RuntimeException {

    private final TranslationId translationId;

    public TranslationNotFoundException(TranslationId translationId) {
        super("Translation not found: " + translationId.value());
        this.translationId = translationId;
    }

    public TranslationId getTranslationId() {
        return translationId;
    }
}
