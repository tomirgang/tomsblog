package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record UpdateTranslationRequest(
        @NotBlank String title, @NotBlank String content) {}
