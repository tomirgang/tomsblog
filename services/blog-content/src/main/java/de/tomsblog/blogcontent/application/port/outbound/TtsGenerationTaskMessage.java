package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;

/**
 * Task message for text-to-speech generation dispatched via RabbitMQ.
 *
 * @req SWR-089
 */
public record TtsGenerationTaskMessage(UUID postId, TenantId tenantId, String locale, String title) {}
