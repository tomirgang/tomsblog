package de.tomsblog.tenantmanagement.adapter.inbound.web;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import de.tomsblog.tenantmanagement.application.service.TenantManagementService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the tenant-management service (ADR-0031).
 *
 * <p>Two filter chains:
 * <ol>
 *   <li>Admin chain (Order 1): form-based login at /tenant/admin/login for SUPERADMIN</li>
 *   <li>Default chain (Order 2): actuator health, static resources</li>
 * </ol>
 *
 * @req SWR-073
 * @req SWR-075
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminProperties.class)
public class SecurityConfiguration {

    private final AdminProperties adminProperties;

    public SecurityConfiguration(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/tenant/admin/**")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/tenant/admin/login")
                        .permitAll()
                        .anyRequest()
                        .hasRole("SUPERADMIN"))
                .formLogin(form -> form.loginPage("/tenant/admin/login")
                        .loginProcessingUrl("/tenant/admin/login")
                        .defaultSuccessUrl("/tenant/admin/tenants", true)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/tenant/admin/logout")
                        .logoutSuccessUrl("/tenant/admin/login")
                        .permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.builder()
                .username(adminProperties.username())
                .password(passwordEncoder().encode(adminProperties.password()))
                .roles("SUPERADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TenantManagementUseCase tenantManagementUseCase(TenantRepository tenantRepository, AuditLogger auditLogger) {
        return new TenantManagementService(tenantRepository, auditLogger);
    }
}
