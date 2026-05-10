package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

/**
 * Audits failed authentication attempts for security monitoring and compliance.
 */
@Component
public class AuthFailureAuditListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuthFailureAuditListener.class);

    private final AuditLogger auditLogger;

    public AuthFailureAuditListener(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName() != null
                ? event.getAuthentication().getName()
                : "unknown";
        LOG.warn("Failed login attempt for user '{}'.", username);
        auditLogger.log(AuditLogEntry.create(
                "system", username, "LOGIN_FAILED", "Authentication", username, "Bad credentials"));
    }
}
