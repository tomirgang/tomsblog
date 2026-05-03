package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;

/** @req SWR-001 @req SWR-040 */
public record UpdatePostCommand(
        PostId postId,
        TenantId tenantId,
        String title,
        String content,
        String socialMediaTitle,
        String socialMediaSummary,
        UUID seriesPreviousPostId,
        UUID seriesNextPostId) {}
