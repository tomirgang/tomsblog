package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;

class AuthFailureAuditListenerTest {

    private final AuditLogger auditLogger = mock(AuditLogger.class);
    private final AuthFailureAuditListener listener = new AuthFailureAuditListener(auditLogger);

    @Test
    @DisplayName("Logs audit entry with username on failed authentication")
    void logsAuditEntryWithUsername() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("bad credentials"));

        listener.onAuthenticationFailure(event);

        verify(auditLogger)
                .log(argThat(entry -> "LOGIN_FAILED".equals(entry.action())
                        && "testuser".equals(entry.entityId())
                        && "testuser".equals(entry.actor())));
    }

    @Test
    @DisplayName("Logs audit entry with 'unknown' when username is null")
    void logsAuditEntryWithUnknownWhenUsernameNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("bad credentials"));

        listener.onAuthenticationFailure(event);

        verify(auditLogger)
                .log(argThat(entry -> "LOGIN_FAILED".equals(entry.action())
                        && "unknown".equals(entry.entityId())
                        && "unknown".equals(entry.actor())));
    }
}
