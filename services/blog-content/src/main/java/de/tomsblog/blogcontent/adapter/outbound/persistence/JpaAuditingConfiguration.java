package de.tomsblog.blogcontent.adapter.outbound.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for automatic population of createdBy/updatedBy fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {}
