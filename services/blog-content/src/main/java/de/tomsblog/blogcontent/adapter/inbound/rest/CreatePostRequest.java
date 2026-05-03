package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** @req SWR-001 @req SWR-035 @req SWR-040 @req SWR-041 */
public record CreatePostRequest(
        @NotNull UUID authorId,
        @NotBlank String title,
        @NotBlank String content,
        String contentType,
        String locale,
        String socialMediaTitle,
        String socialMediaSummary,
        UUID seriesPreviousPostId,
        UUID seriesNextPostId,
        LocalDate featuredFrom,
        LocalDate featuredUntil) {}
