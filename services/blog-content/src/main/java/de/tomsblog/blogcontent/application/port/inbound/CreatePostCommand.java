package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;

public record CreatePostCommand(TenantId tenantId, AuthorId authorId, String title, String content, String locale) {
}
