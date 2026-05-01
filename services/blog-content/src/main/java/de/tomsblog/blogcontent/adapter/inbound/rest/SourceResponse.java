package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.domain.model.Source;

/** @req SWR-012 */
public record SourceResponse(String url, String title) {

    public static SourceResponse from(Source source) {
        return new SourceResponse(source.url(), source.title());
    }
}
