package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** @req SWR-001 @req SWR-040 */
public record UpdatePostRequest(
        @NotBlank String title,
        @NotBlank String content,
        String socialMediaTitle,
        String socialMediaSummary,
        UUID seriesPreviousPostId,
        UUID seriesNextPostId) {}
