package de.tomsblog.usermanagement.adapter.inbound.rest;

import de.tomsblog.usermanagement.adapter.inbound.web.AdminProperties;
import de.tomsblog.usermanagement.adapter.inbound.web.DefaultTenantFilter;
import de.tomsblog.usermanagement.adapter.inbound.web.SyncingOidcUserService;
import de.tomsblog.usermanagement.adapter.inbound.web.TenantAwareClientRegistrationRepository;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the user-management service (ADR-0032).
 *
 * <p>Three filter chains:
 * <ol>
 *   <li>API chain (Order 1): stateless, API-key authentication for service-to-service calls</li>
 *   <li>Break-glass admin chain (Order 2): form-based login at /auth/admin/login for SUPERADMIN</li>
 *   <li>Auth UI chain (Order 3): OIDC + form login for /auth/** pages</li>
 * </ol>
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-043
 * @req SWR-044
 * @req SWR-064
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
    AdminProperties.class,
    org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.class
})
public class SecurityConfiguration {

    @Value("${service.api-key:#{null}}")
    private String apiKey;

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfiguration(AdminProperties adminProperties, PasswordEncoder passwordEncoder) {
        this.adminProperties = adminProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * API filter chain: stateless, API-key authentication for /api/** and gRPC health.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint((request, response, authException) -> response.sendError(401)))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Break-glass admin filter chain: form-based login at /auth/admin/login for SUPERADMIN.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/auth/admin/**")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/admin/login")
                        .permitAll()
                        .requestMatchers("/auth/admin/switch-tenant")
                        .hasRole("SUPERADMIN")
                        .requestMatchers("/auth/admin/users", "/auth/admin/users/**", "/auth/admin/settings")
                        .hasAnyRole("SUPERADMIN", "ADMIN")
                        .anyRequest()
                        .hasRole("SUPERADMIN"))
                .formLogin(form -> form.loginPage("/auth/admin/login")
                        .loginProcessingUrl("/auth/admin/login")
                        .defaultSuccessUrl("/auth/admin/users", true)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/auth/admin/logout")
                        .logoutSuccessUrl("/auth/login")
                        .permitAll());

        return http.build();
    }

    /**
     * Auth UI filter chain: OIDC login, form login, registration pages.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain authUiFilterChain(HttpSecurity http, SyncingOidcUserService oidcUserService)
            throws Exception {
        http.securityMatcher("/auth/**", "/oauth2/authorization/**", "/login/oauth2/code/**")
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/auth/login", "/auth/register", "/auth/registration-success")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2Login(oauth2 -> oauth2.loginPage("/auth/login")
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService)))
                .formLogin(form -> form.loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout ->
                        logout.logoutUrl("/auth/logout").logoutSuccessUrl("/").permitAll());

        return http.build();
    }

    /**
     * Default filter chain: actuator health, Swagger, static resources.
     */
    @Bean
    @Order(4)
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

    /**
     * In-memory user details for the SUPERADMIN break-glass login.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.builder()
                .username(adminProperties.username())
                .password(passwordEncoder.encode(adminProperties.password()))
                .roles("SUPERADMIN", "ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SyncingOidcUserService syncingOidcUserService(
            UserProfileUseCase userProfileUseCase,
            TenantSettingsUseCase tenantSettingsUseCase,
            DefaultTenantFilter defaultTenantFilter) {
        return new SyncingOidcUserService(
                userProfileUseCase,
                tenantSettingsUseCase,
                java.util.UUID.fromString(defaultTenantFilter.getDefaultTenantId()));
    }

    /**
     * Tenant-aware client registration repository that loads OIDC config per tenant (SWR-063).
     * Falls back to the global application.yml config when no tenant-specific config exists.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.authentik", name = "client-id")
    public ClientRegistrationRepository clientRegistrationRepository(
            TenantSettingsUseCase tenantSettingsUseCase,
            DefaultTenantFilter defaultTenantFilter,
            org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties
                    oAuth2ClientProperties) {
        var authentikReg = oAuth2ClientProperties.getRegistration().get("authentik");
        var authentikProvider = oAuth2ClientProperties.getProvider().get("authentik");
        ClientRegistration fallback = ClientRegistration.withRegistrationId("authentik")
                .clientId(authentikReg.getClientId())
                .clientSecret(authentikReg.getClientSecret())
                .authorizationGrantType(new org.springframework.security.oauth2.core.AuthorizationGrantType(
                        authentikReg.getAuthorizationGrantType()))
                .redirectUri(authentikReg.getRedirectUri())
                .scope(authentikReg.getScope())
                .authorizationUri(authentikProvider.getAuthorizationUri())
                .tokenUri(authentikProvider.getTokenUri())
                .userInfoUri(authentikProvider.getUserInfoUri())
                .jwkSetUri(authentikProvider.getJwkSetUri())
                .build();
        return new TenantAwareClientRegistrationRepository(
                tenantSettingsUseCase, fallback, defaultTenantFilter.getDefaultTenantId());
    }
}
