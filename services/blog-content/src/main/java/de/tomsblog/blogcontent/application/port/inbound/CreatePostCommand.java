package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-001 @req SWR-035 */
public record CreatePostCommand(
        TenantId tenantId,
        AuthorId authorId,
        String title,
        String content,
        String contentType,
        String locale,
        String socialMediaTitle,
        String socialMediaSummary) {}
