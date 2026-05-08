package de.tomsblog.blogcontent.application.port.inbound;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogSearchCriteriaTest {

    @Test
    @DisplayName("SWR-087: hasFilters returns false when all fields are null")
    void hasFilters_allNull_returnsFalse() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, null, null, null, null);

        assertThat(criteria.hasFilters()).isFalse();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when action is set")
    void hasFilters_actionSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria("POST_CREATED", null, null, null, null, null);

        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when entityType is set")
    void hasFilters_entityTypeSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, "Post", null, null, null, null);

        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when actor is set")
    void hasFilters_actorSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, "admin", null, null, null);

        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when from date is set")
    void hasFilters_fromSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, null, Instant.now(), null, null);

        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when to date is set")
    void hasFilters_toSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, null, null, Instant.now(), null);

        assertThat(criteria.hasFilters()).isTrue();
    }

    @Test
    @DisplayName("SWR-087: hasFilters returns true when searchTerm is set")
    void hasFilters_searchTermSet_returnsTrue() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, null, null, null, "query");

        assertThat(criteria.hasFilters()).isTrue();
    }
}
