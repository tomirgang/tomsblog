package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** @req SWR-001 @req SWR-035 @req SWR-040 @req SWR-041 */
public record CreatePostRequest(
        @NotNull UUID authorId,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 1_000_000) String content,
        @Size(max = 50) String contentType,
        @Size(max = 10) String locale,
        @Size(max = 500) String socialMediaTitle,
        @Size(max = 2000) String socialMediaSummary,
        UUID seriesPreviousPostId,
        UUID seriesNextPostId,
        LocalDate featuredFrom,
        LocalDate featuredUntil) {}
