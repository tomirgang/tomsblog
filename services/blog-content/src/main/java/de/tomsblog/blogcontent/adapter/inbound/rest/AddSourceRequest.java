package de.tomsblog.blogcontent.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @req SWR-012 */
public record AddSourceRequest(
        @NotBlank @Size(max = 2000) String url,
        @NotBlank @Size(max = 500) String title) {}
