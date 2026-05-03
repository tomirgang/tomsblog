package de.tomsblog.usermanagement;

import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.application.service.TenantSettingsService;
import de.tomsblog.usermanagement.application.service.UserProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserManagementConfiguration {

    @Bean
    public UserProfileUseCase userProfileUseCase(
            UserProfileRepository userProfileRepository, TenantSettingsRepository tenantSettingsRepository) {
        return new UserProfileService(userProfileRepository, tenantSettingsRepository);
    }

    @Bean
    public TenantSettingsUseCase tenantSettingsUseCase(TenantSettingsRepository tenantSettingsRepository) {
        return new TenantSettingsService(tenantSettingsRepository);
    }
}
