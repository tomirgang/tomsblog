package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.blogcontent.application.port.outbound.AuditLogQueryRepository;
import de.tomsblog.shared.audit.AuditLogEntry;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * JPA adapter for querying audit log entries using specifications.
 *
 * @req SWR-087
 * @req SWA-036
 */
@Component
public class JpaAuditLogQueryAdapter implements AuditLogQueryRepository {

    private final SpringDataAuditLogRepository repository;

    public JpaAuditLogQueryAdapter(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<AuditLogEntry> findByTenantId(String tenantId, Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = tenantEquals(tenantId);
        return repository.findAll(spec, pageable).map(this::toDomain);
    }

    @Override
    public Page<AuditLogEntry> findByTenantIdAndCriteria(
            String tenantId, AuditLogSearchCriteria criteria, Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = buildSpecification(tenantId, criteria);
        return repository.findAll(spec, pageable).map(this::toDomain);
    }

    @Override
    public List<String> findDistinctActions(String tenantId) {
        return repository.findDistinctActionsByTenantId(tenantId);
    }

    @Override
    public List<String> findDistinctEntityTypes(String tenantId) {
        return repository.findDistinctEntityTypesByTenantId(tenantId);
    }

    @Override
    public List<String> findDistinctActors(String tenantId) {
        return repository.findDistinctActorsByTenantId(tenantId);
    }

    private Specification<AuditLogJpaEntity> buildSpecification(String tenantId, AuditLogSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (criteria.action() != null) {
                predicates.add(cb.equal(root.get("action"), criteria.action()));
            }
            if (criteria.entityType() != null) {
                predicates.add(cb.equal(root.get("entityType"), criteria.entityType()));
            }
            if (criteria.actor() != null) {
                predicates.add(cb.equal(root.get("actor"), criteria.actor()));
            }
            if (criteria.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), criteria.to()));
            }
            if (criteria.searchTerm() != null) {
                String pattern = "%" + criteria.searchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("actor")), pattern),
                        cb.like(cb.lower(root.get("entityType")), pattern),
                        cb.like(cb.lower(root.get("entityId")), pattern),
                        cb.like(cb.lower(root.get("details")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<AuditLogJpaEntity> tenantEquals(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private AuditLogEntry toDomain(AuditLogJpaEntity entity) {
        return new AuditLogEntry(
                entity.getId(),
                entity.getTimestamp(),
                entity.getTenantId(),
                entity.getActor(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getDetails());
    }
}
