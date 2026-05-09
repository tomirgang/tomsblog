package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;

/**
 * Task message for web snapshot creation dispatched via RabbitMQ.
 *
 * @req SWR-089
 */
public record SnapshotTaskMessage(UUID postId, TenantId tenantId, String url, String sourceTitle) {}
