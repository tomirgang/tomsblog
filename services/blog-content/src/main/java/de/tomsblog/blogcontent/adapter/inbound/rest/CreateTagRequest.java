package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-020 */
public record CreateTagRequest(@NotBlank String name) {}
