package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.TranslationApprovedEvent;
import de.tomsblog.blogcontent.domain.event.TranslationCreatedEvent;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TranslationTest {

    private final TenantId tenantId = TenantId.generate();
    private final PostId postId = PostId.generate();

    @Test
    @DisplayName("SWR-005: Create manual translation starts in DRAFT status")
    void createManualTranslationStartsInDraft() {
        Translation translation = Translation.createManual(
                postId, tenantId, PostLocale.english(), "Translated Title", "Translated Content");

        assertThat(translation.getId()).isNotNull();
        assertThat(translation.getPostId()).isEqualTo(postId);
        assertThat(translation.getTenantId()).isEqualTo(tenantId);
        assertThat(translation.getLocale()).isEqualTo(PostLocale.english());
        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.DRAFT);
        assertThat(translation.getSource()).isEqualTo(TranslationSource.MANUAL);
    }

    @Test
    @DisplayName("SWR-004: Create AI translation starts in REVIEW_PENDING status")
    void createAiTranslationStartsInReviewPending() {
        Translation translation =
                Translation.createFromAi(postId, tenantId, PostLocale.english(), "AI Title", "AI Content");

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.REVIEW_PENDING);
        assertThat(translation.getSource()).isEqualTo(TranslationSource.AI_GENERATED);
    }

    @Test
    @DisplayName("SWR-005: Manual translation registers TranslationCreatedEvent")
    void manualTranslationRegistersEvent() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        assertThat(translation.getDomainEvents()).hasSize(1);
        assertThat(translation.getDomainEvents().getFirst()).isInstanceOf(TranslationCreatedEvent.class);
    }

    @Test
    @DisplayName("SWR-004: Approve translation changes status and registers event")
    void approveTranslationChangesStatus() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.clearDomainEvents();

        translation.approve();

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.APPROVED);
        assertThat(translation.getDomainEvents()).hasSize(1);
        assertThat(translation.getDomainEvents().getFirst()).isInstanceOf(TranslationApprovedEvent.class);
    }

    @Test
    @DisplayName("SWR-004: Reject translation changes status")
    void rejectTranslationChangesStatus() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");

        translation.reject();

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.REJECTED);
    }

    @Test
    @DisplayName("Cannot approve an already approved translation")
    void cannotApproveAlreadyApproved() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.approve();

        assertThatThrownBy(translation::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already approved");
    }

    @Test
    @DisplayName("Cannot reject an already rejected translation")
    void cannotRejectAlreadyRejected() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.reject();

        assertThatThrownBy(translation::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already rejected");
    }

    @Test
    @DisplayName("Cannot approve a rejected translation")
    void cannotApproveRejected() {
        Translation translation = Translation.createFromAi(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.reject();

        assertThatThrownBy(translation::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve a rejected translation");
    }

    @Test
    @DisplayName("Update content on approved translation reverts to REVIEW_PENDING")
    void updateContentOnApprovedRevertsToReviewPending() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.approve();

        translation.updateContent("Updated Title", "Updated Content");

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.REVIEW_PENDING);
        assertThat(translation.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("Create translation with blank title throws exception")
    void createTranslationWithBlankTitleThrows() {
        assertThatThrownBy(() -> Translation.createManual(postId, tenantId, PostLocale.english(), "", "Content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
    }

    @Test
    @DisplayName("Create translation with blank content throws exception")
    void createTranslationWithBlankContentThrows() {
        assertThatThrownBy(() -> Translation.createManual(postId, tenantId, PostLocale.english(), "Title", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content must not be blank");
    }

    @Test
    @DisplayName("Cannot reject an approved translation")
    void cannotRejectApproved() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        translation.approve();

        assertThatThrownBy(translation::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reject an approved translation");
    }

    @Test
    @DisplayName("Update content on DRAFT translation keeps DRAFT status")
    void updateContentOnDraftKeepsDraft() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        translation.updateContent("New Title", "New Content");

        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.DRAFT);
        assertThat(translation.getTitle()).isEqualTo("New Title");
        assertThat(translation.getContent()).isEqualTo("New Content");
    }

    @Test
    @DisplayName("Update content with blank title throws")
    void updateContentWithBlankTitleThrows() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        assertThatThrownBy(() -> translation.updateContent("", "Content")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Update content with null title throws")
    void updateContentWithNullTitleThrows() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        assertThatThrownBy(() -> translation.updateContent(null, "Content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Update content with blank content throws")
    void updateContentWithBlankContentThrows() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        assertThatThrownBy(() -> translation.updateContent("Title", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Update content with null content throws")
    void updateContentWithNullContentThrows() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");

        assertThatThrownBy(() -> translation.updateContent("Title", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Create translation with null title throws exception")
    void createTranslationWithNullTitleThrows() {
        assertThatThrownBy(() -> Translation.createManual(postId, tenantId, PostLocale.english(), null, "Content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Create translation with null content throws exception")
    void createTranslationWithNullContentThrows() {
        assertThatThrownBy(() -> Translation.createManual(postId, tenantId, PostLocale.english(), "Title", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Translation getters return correct values")
    void translationGetters() {
        Translation translation =
                Translation.createManual(postId, tenantId, PostLocale.english(), "My Title", "My Content");

        assertThat(translation.getTitle()).isEqualTo("My Title");
        assertThat(translation.getContent()).isEqualTo("My Content");
        assertThat(translation.getTranslatedAt()).isNotNull();
    }
}
