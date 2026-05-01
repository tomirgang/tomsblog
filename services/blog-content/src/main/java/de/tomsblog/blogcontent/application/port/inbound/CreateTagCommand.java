package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-020 */
public record CreateTagCommand(TenantId tenantId, String name) {}
