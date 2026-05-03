package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration with OIDC login (Authentik) and break-glass admin form login.
 *
 * <p>Two filter chains:
 * <ol>
 *   <li>{@code /admin/login} chain (Order 1): form-based login for SUPERADMIN (break-glass)</li>
 *   <li>Default chain (Order 2): OIDC login via Authentik + public read access</li>
 * </ol>
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-044
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AdminProperties.class})
public class SecurityConfiguration {

    private final AdminProperties adminProperties;

    public SecurityConfiguration(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    /**
     * Break-glass admin filter chain: form-based login at /admin/login for SUPERADMIN.
     * Admin pages (/admin/users, /admin/settings) accessible by ADMIN and SUPERADMIN.
     * Tenant switching only for SUPERADMIN.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/admin/login")
                        .permitAll()
                        .requestMatchers("/admin/switch-tenant")
                        .hasRole("SUPERADMIN")
                        .requestMatchers("/admin/users", "/admin/users/**", "/admin/settings")
                        .hasAnyRole("SUPERADMIN", "ADMIN")
                        .anyRequest()
                        .hasRole("SUPERADMIN"))
                .formLogin(form -> form.loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/posts", true)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/posts")
                        .permitAll());

        return http.build();
    }

    /**
     * Main filter chain: OIDC login via Authentik, form login as fallback, public read access.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http, SyncingOidcUserService oidcUserService)
            throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // Public: static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        // Public: login/logout
                        .requestMatchers("/login", "/logout")
                        .permitAll()
                        // Public: actuator health
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        // Public: Swagger UI and API docs (disabled in production via profile)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                        .permitAll()
                        // API: role-based access control
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/**")
                        .hasAnyRole("AUTHOR", "ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**")
                        .hasAnyRole("AUTHOR", "ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/**")
                        .hasAnyRole("AUTHOR", "ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")
                        // Protected: admin-only paths (evaluated before public slug pattern)
                        .requestMatchers("/posts/new", "/posts/*/edit")
                        .authenticated()
                        // Public: blog reading (GET only)
                        .requestMatchers(HttpMethod.GET, "/")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/{slug}")
                        .permitAll()
                        // Everything else requires authentication (default-deny)
                        .anyRequest()
                        .authenticated())
                .oauth2Login(oauth2 -> oauth2.loginPage("/login")
                        .defaultSuccessUrl("/posts", true)
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService)))
                .formLogin(form -> form.loginPage("/login")
                        .defaultSuccessUrl("/posts", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/posts").permitAll())
                .httpBasic(basic -> {})
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; "
                                + "script-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; "
                                + "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://unpkg.com; "
                                + "img-src 'self' data: https:; "
                                + "font-src 'self'; "
                                + "connect-src 'self'; "
                                + "frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(
                                hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));

        return http.build();
    }

    /**
     * In-memory user details for the SUPERADMIN break-glass login.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.builder()
                .username(adminProperties.username())
                .password(passwordEncoder().encode(adminProperties.password()))
                .roles("SUPERADMIN", "ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SyncingOidcUserService syncingOidcUserService(
            UserManagementClient userManagementClient, DefaultTenantFilter defaultTenantFilter) {
        return new SyncingOidcUserService(
                userManagementClient, java.util.UUID.fromString(defaultTenantFilter.getDefaultTenantId()));
    }
}
