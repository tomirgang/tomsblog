package de.tomsblog.blogcontent.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.inbound.CreateTranslationCommand;
import de.tomsblog.blogcontent.application.port.inbound.UpdateTranslationCommand;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.TranslationRepository;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private EventPublisher eventPublisher;

    private TranslationService translationService;

    private final TenantId tenantId = TenantId.generate();
    private final PostId postId = PostId.generate();

    @BeforeEach
    void setUp() {
        translationService = new TranslationService(translationRepository, eventPublisher);
    }

    @Test
    @DisplayName("SWR-005: createManualTranslation saves and publishes event")
    void createManualTranslation_savesAndPublishes() {
        when(translationRepository.save(any(Translation.class))).thenAnswer(inv -> inv.getArgument(0));
        List<DomainEvent> captured = new ArrayList<>();
        doAnswer(inv -> {
                    captured.addAll(inv.getArgument(0));
                    return null;
                })
                .when(eventPublisher)
                .publish(any());

        CreateTranslationCommand command =
                new CreateTranslationCommand(postId, tenantId, "en", "English Title", "English Content");

        Translation result = translationService.createManualTranslation(command);

        assertThat(result.getTitle()).isEqualTo("English Title");
        assertThat(result.getSource()).isEqualTo(TranslationSource.MANUAL);
        assertThat(result.getStatus()).isEqualTo(TranslationStatus.DRAFT);
        verify(translationRepository).save(any(Translation.class));
        assertThat(captured).hasSize(1);
    }

    @Test
    @DisplayName("SWR-004: createAiTranslation saves and publishes event")
    void createAiTranslation_savesAndPublishes() {
        when(translationRepository.save(any(Translation.class))).thenAnswer(inv -> inv.getArgument(0));
        List<DomainEvent> captured = new ArrayList<>();
        doAnswer(inv -> {
                    captured.addAll(inv.getArgument(0));
                    return null;
                })
                .when(eventPublisher)
                .publish(any());

        CreateTranslationCommand command =
                new CreateTranslationCommand(postId, tenantId, "en", "AI Title", "AI Content");

        Translation result = translationService.createAiTranslation(command);

        assertThat(result.getTitle()).isEqualTo("AI Title");
        assertThat(result.getSource()).isEqualTo(TranslationSource.AI_GENERATED);
        assertThat(result.getStatus()).isEqualTo(TranslationStatus.REVIEW_PENDING);
        verify(translationRepository).save(any(Translation.class));
        assertThat(captured).hasSize(1);
    }

    @Test
    @DisplayName("SWR-005: updateTranslation changes content")
    void updateTranslation_changesContent() {
        Translation translation =
                Translation.createManual(postId, tenantId, PostLocale.english(), "Original", "Original Content");
        when(translationRepository.findByIdAndTenantId(translation.getId(), tenantId))
                .thenReturn(Optional.of(translation));
        when(translationRepository.save(any(Translation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTranslationCommand command =
                new UpdateTranslationCommand(translation.getId(), tenantId, "Updated", "Updated Content");

        Translation result = translationService.updateTranslation(command);

        assertThat(result.getTitle()).isEqualTo("Updated");
        assertThat(result.getContent()).isEqualTo("Updated Content");
    }

    @Test
    @DisplayName("SWR-005: updateTranslation throws when not found")
    void updateTranslation_throwsWhenNotFound() {
        TranslationId translationId = TranslationId.generate();
        when(translationRepository.findByIdAndTenantId(translationId, tenantId)).thenReturn(Optional.empty());

        UpdateTranslationCommand command = new UpdateTranslationCommand(translationId, tenantId, "Title", "Content");

        assertThatThrownBy(() -> translationService.updateTranslation(command))
                .isInstanceOf(TranslationNotFoundException.class)
                .satisfies(ex -> assertThat(((TranslationNotFoundException) ex).getTranslationId())
                        .isEqualTo(translationId));
    }

    @Test
    @DisplayName("SWR-004: approveTranslation changes status and publishes event")
    void approveTranslation_changesStatusAndPublishes() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.clearDomainEvents();
        when(translationRepository.findByIdAndTenantId(translation.getId(), tenantId))
                .thenReturn(Optional.of(translation));
        when(translationRepository.save(any(Translation.class))).thenAnswer(inv -> inv.getArgument(0));
        List<DomainEvent> captured = new ArrayList<>();
        doAnswer(inv -> {
                    captured.addAll(inv.getArgument(0));
                    return null;
                })
                .when(eventPublisher)
                .publish(any());

        translationService.approveTranslation(translation.getId(), tenantId);

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.APPROVED);
        assertThat(captured).hasSize(1);
    }

    @Test
    @DisplayName("SWR-004: approveTranslation throws when not found")
    void approveTranslation_throwsWhenNotFound() {
        TranslationId translationId = TranslationId.generate();
        when(translationRepository.findByIdAndTenantId(translationId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationService.approveTranslation(translationId, tenantId))
                .isInstanceOf(TranslationNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-004: rejectTranslation changes status")
    void rejectTranslation_changesStatus() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");
        when(translationRepository.findByIdAndTenantId(translation.getId(), tenantId))
                .thenReturn(Optional.of(translation));
        when(translationRepository.save(any(Translation.class))).thenAnswer(inv -> inv.getArgument(0));

        translationService.rejectTranslation(translation.getId(), tenantId);

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.REJECTED);
    }

    @Test
    @DisplayName("SWR-004: rejectTranslation throws when not found")
    void rejectTranslation_throwsWhenNotFound() {
        TranslationId translationId = TranslationId.generate();
        when(translationRepository.findByIdAndTenantId(translationId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationService.rejectTranslation(translationId, tenantId))
                .isInstanceOf(TranslationNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-005: deleteTranslation removes it")
    void deleteTranslation_removesTranslation() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        when(translationRepository.findByIdAndTenantId(translation.getId(), tenantId))
                .thenReturn(Optional.of(translation));

        translationService.deleteTranslation(translation.getId(), tenantId);

        verify(translationRepository).deleteByIdAndTenantId(translation.getId(), tenantId);
    }

    @Test
    @DisplayName("SWR-005: deleteTranslation throws when not found")
    void deleteTranslation_throwsWhenNotFound() {
        TranslationId translationId = TranslationId.generate();
        when(translationRepository.findByIdAndTenantId(translationId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationService.deleteTranslation(translationId, tenantId))
                .isInstanceOf(TranslationNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-005: getTranslation returns translation")
    void getTranslation_returnsTranslation() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        when(translationRepository.findByIdAndTenantId(translation.getId(), tenantId))
                .thenReturn(Optional.of(translation));

        Translation result = translationService.getTranslation(translation.getId(), tenantId);

        assertThat(result.getId()).isEqualTo(translation.getId());
    }

    @Test
    @DisplayName("SWR-005: getTranslation throws when not found")
    void getTranslation_throwsWhenNotFound() {
        TranslationId translationId = TranslationId.generate();
        when(translationRepository.findByIdAndTenantId(translationId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationService.getTranslation(translationId, tenantId))
                .isInstanceOf(TranslationNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-005: listTranslations returns all for post and tenant")
    void listTranslations_returnsAll() {
        Translation t1 = Translation.createManual(postId, tenantId, PostLocale.english(), "Title 1", "Content 1");
        Translation t2 = Translation.createManual(postId, tenantId, PostLocale.german(), "Titel 2", "Inhalt 2");
        when(translationRepository.findAllByPostIdAndTenantId(postId, tenantId)).thenReturn(List.of(t1, t2));

        List<Translation> result = translationService.listTranslations(postId, tenantId);

        assertThat(result).hasSize(2);
    }
}
