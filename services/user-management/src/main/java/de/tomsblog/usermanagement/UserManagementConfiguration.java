package de.tomsblog.usermanagement;

import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.application.service.UserProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserManagementConfiguration {

    @Bean
    public UserProfileUseCase userProfileUseCase(UserProfileRepository userProfileRepository) {
        return new UserProfileService(userProfileRepository);
    }
}
