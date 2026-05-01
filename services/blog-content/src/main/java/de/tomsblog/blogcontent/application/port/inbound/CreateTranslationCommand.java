package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-021 */
public record CreateTranslationCommand(PostId postId, TenantId tenantId, String locale, String title, String content) {}
