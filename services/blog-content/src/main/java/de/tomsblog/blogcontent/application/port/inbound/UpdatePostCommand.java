package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-001 */
public record UpdatePostCommand(
        PostId postId,
        TenantId tenantId,
        String title,
        String content,
        String socialMediaTitle,
        String socialMediaSummary) {}
