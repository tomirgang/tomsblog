package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;

/** @req SWR-021 */
public record UpdateTranslationCommand(TranslationId translationId, TenantId tenantId, String title, String content) {}
