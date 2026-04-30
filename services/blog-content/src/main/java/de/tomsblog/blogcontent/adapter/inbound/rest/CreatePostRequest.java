package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePostRequest(
        @NotNull UUID authorId,
        @NotBlank String title,
        @NotBlank String content,
        String locale) {
}
