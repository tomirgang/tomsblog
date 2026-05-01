package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;

public record RenameTagCommand(TagId tagId, TenantId tenantId, String newName) {}
