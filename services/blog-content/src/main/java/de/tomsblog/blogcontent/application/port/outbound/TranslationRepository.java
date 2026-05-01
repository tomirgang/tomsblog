package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for translation persistence.
 *
 * @req SWR-021
 * @req SWR-003
 */
public interface TranslationRepository {

    Translation save(Translation translation);

    Optional<Translation> findByIdAndTenantId(TranslationId id, TenantId tenantId);

    List<Translation> findAllByPostIdAndTenantId(PostId postId, TenantId tenantId);

    void deleteByIdAndTenantId(TranslationId id, TenantId tenantId);
}
