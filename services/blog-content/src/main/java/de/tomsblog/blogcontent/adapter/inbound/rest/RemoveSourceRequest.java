package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-012 */
public record RemoveSourceRequest(@NotBlank String url, @NotBlank String title) {}
