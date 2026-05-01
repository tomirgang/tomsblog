package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;

public record AddSourceCommand(PostId postId, TenantId tenantId, String url, String title) {}
