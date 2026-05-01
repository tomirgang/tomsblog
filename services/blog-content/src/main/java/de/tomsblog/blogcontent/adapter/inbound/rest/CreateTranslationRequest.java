package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-021 */
public record CreateTranslationRequest(
        @NotBlank String locale,
        @NotBlank String title,
        @NotBlank String content,
        String source) {}
