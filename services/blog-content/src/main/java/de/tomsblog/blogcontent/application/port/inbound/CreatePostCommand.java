package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.time.LocalDate;
import java.util.UUID;

/** @req SWR-001 @req SWR-035 @req SWR-040 @req SWR-041 */
public record CreatePostCommand(
        TenantId tenantId,
        AuthorId authorId,
        String title,
        String content,
        String contentType,
        String locale,
        String socialMediaTitle,
        String socialMediaSummary,
        UUID seriesPreviousPostId,
        UUID seriesNextPostId,
        LocalDate featuredFrom,
        LocalDate featuredUntil) {}
