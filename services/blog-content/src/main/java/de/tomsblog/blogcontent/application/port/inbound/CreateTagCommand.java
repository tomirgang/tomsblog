package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;

public record CreateTagCommand(TenantId tenantId, String name) {}
