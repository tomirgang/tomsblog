package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;

/**
 * Task message for AI translation requests dispatched via RabbitMQ.
 *
 * @req SWR-089
 */
public record TranslationTaskMessage(
        UUID postId, TenantId tenantId, String sourceLocale, String targetLocale, String content) {}
