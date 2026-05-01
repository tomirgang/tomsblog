package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(@NotBlank String name) {}
