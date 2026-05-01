package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-012 */
public record RemoveSourceCommand(PostId postId, TenantId tenantId, String url, String title) {}
