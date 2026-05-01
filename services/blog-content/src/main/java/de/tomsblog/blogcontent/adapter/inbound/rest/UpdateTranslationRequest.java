package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-021 */
public record UpdateTranslationRequest(
        @NotBlank String title, @NotBlank String content) {}
