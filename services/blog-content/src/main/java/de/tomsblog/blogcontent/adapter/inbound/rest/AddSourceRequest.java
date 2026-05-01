package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record AddSourceRequest(@NotBlank String url, @NotBlank String title) {}
