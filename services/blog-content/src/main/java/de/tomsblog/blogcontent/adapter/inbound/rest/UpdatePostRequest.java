package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostRequest(
        @NotBlank String title, @NotBlank String content, String socialMediaTitle, String socialMediaSummary) {}
