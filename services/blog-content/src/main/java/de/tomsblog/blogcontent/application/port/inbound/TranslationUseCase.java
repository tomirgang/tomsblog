package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Inbound port for translation management use cases.
 *
 * @req SWR-021
 * @req SWR-004
 * @req SWR-005
 */
public interface TranslationUseCase {

    Translation createManualTranslation(CreateTranslationCommand command);

    Translation createAiTranslation(CreateTranslationCommand command);

    Translation updateTranslation(UpdateTranslationCommand command);

    void approveTranslation(TranslationId translationId, TenantId tenantId);

    void rejectTranslation(TranslationId translationId, TenantId tenantId);

    void deleteTranslation(TranslationId translationId, TenantId tenantId);

    Translation getTranslation(TranslationId translationId, TenantId tenantId);

    List<Translation> listTranslations(PostId postId, TenantId tenantId);
}
