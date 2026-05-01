package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-001 */
public record UpdatePostRequest(
        @NotBlank String title, @NotBlank String content, String socialMediaTitle, String socialMediaSummary) {}
