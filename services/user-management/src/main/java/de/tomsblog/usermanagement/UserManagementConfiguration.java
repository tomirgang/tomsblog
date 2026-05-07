package de.tomsblog.usermanagement;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.application.service.TenantSettingsService;
import de.tomsblog.usermanagement.application.service.UserProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserManagementConfiguration {

    @Bean
    public UserProfileUseCase userProfileUseCase(
            UserProfileRepository userProfileRepository,
            TenantSettingsRepository tenantSettingsRepository,
            AuditLogger auditLogger,
            PasswordEncoder passwordEncoder) {
        return new UserProfileService(userProfileRepository, tenantSettingsRepository, auditLogger, passwordEncoder);
    }

    @Bean
    public TenantSettingsUseCase tenantSettingsUseCase(
            TenantSettingsRepository tenantSettingsRepository, AuditLogger auditLogger) {
        return new TenantSettingsService(tenantSettingsRepository, auditLogger);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
