package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.tenant.TenantId;

public class TranslationMapper {

    private TranslationMapper() {}

    public static TranslationJpaEntity toEntity(Translation translation) {
        TranslationJpaEntity entity = new TranslationJpaEntity();
        entity.setId(translation.getId().value());
        entity.setPostId(translation.getPostId().value());
        entity.setTenantId(translation.getTenantId().value());
        entity.setLocale(translation.getLocale().languageTag());
        entity.setTitle(translation.getTitle());
        entity.setContent(translation.getContent());
        entity.setStatus(translation.getStatus().name());
        entity.setSource(translation.getSource().name());
        entity.setTranslatedAt(translation.getTranslatedAt());
        return entity;
    }

    public static Translation toDomain(TranslationJpaEntity entity) {
        return Translation.reconstitute(
                TranslationId.of(entity.getId()),
                PostId.of(entity.getPostId()),
                TenantId.of(entity.getTenantId()),
                PostLocale.of(entity.getLocale()),
                entity.getTitle(),
                entity.getContent(),
                TranslationStatus.valueOf(entity.getStatus()),
                TranslationSource.valueOf(entity.getSource()),
                entity.getTranslatedAt());
    }
}
