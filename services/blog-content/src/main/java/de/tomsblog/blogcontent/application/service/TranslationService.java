package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.CreateTranslationCommand;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdateTranslationCommand;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.TranslationRepository;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Application service orchestrating translation use cases.
 *
 * @req SWR-021
 * @req SWR-004
 * @req SWR-005
 * @req SWR-009
 */
public class TranslationService implements TranslationUseCase {

    private final TranslationRepository translationRepository;
    private final EventPublisher eventPublisher;

    public TranslationService(TranslationRepository translationRepository, EventPublisher eventPublisher) {
        this.translationRepository = translationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Translation createManualTranslation(CreateTranslationCommand command) {
        PostLocale locale = PostLocale.of(command.locale());
        Translation translation = Translation.createManual(
                command.postId(), command.tenantId(), locale, command.title(), command.content());
        Translation saved = translationRepository.save(translation);
        eventPublisher.publish(translation.getDomainEvents());
        translation.clearDomainEvents();
        return saved;
    }

    @Override
    public Translation createAiTranslation(CreateTranslationCommand command) {
        PostLocale locale = PostLocale.of(command.locale());
        Translation translation = Translation.createFromAi(
                command.postId(), command.tenantId(), locale, command.title(), command.content());
        Translation saved = translationRepository.save(translation);
        eventPublisher.publish(translation.getDomainEvents());
        translation.clearDomainEvents();
        return saved;
    }

    @Override
    public Translation updateTranslation(UpdateTranslationCommand command) {
        Translation translation = findOrThrow(command.translationId(), command.tenantId());
        translation.updateContent(command.title(), command.content());
        return translationRepository.save(translation);
    }

    @Override
    public void approveTranslation(TranslationId translationId, TenantId tenantId) {
        Translation translation = findOrThrow(translationId, tenantId);
        translation.approve();
        translationRepository.save(translation);
        eventPublisher.publish(translation.getDomainEvents());
        translation.clearDomainEvents();
    }

    @Override
    public void rejectTranslation(TranslationId translationId, TenantId tenantId) {
        Translation translation = findOrThrow(translationId, tenantId);
        translation.reject();
        translationRepository.save(translation);
    }

    @Override
    public void deleteTranslation(TranslationId translationId, TenantId tenantId) {
        findOrThrow(translationId, tenantId);
        translationRepository.deleteByIdAndTenantId(translationId, tenantId);
    }

    @Override
    public Translation getTranslation(TranslationId translationId, TenantId tenantId) {
        return findOrThrow(translationId, tenantId);
    }

    @Override
    public List<Translation> listTranslations(PostId postId, TenantId tenantId) {
        return translationRepository.findAllByPostIdAndTenantId(postId, tenantId);
    }

    private Translation findOrThrow(TranslationId translationId, TenantId tenantId) {
        return translationRepository
                .findByIdAndTenantId(translationId, tenantId)
                .orElseThrow(() -> new TranslationNotFoundException(translationId));
    }
}
