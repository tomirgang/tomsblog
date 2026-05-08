package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Blog Content Service (ADR-0032).
 *
 * <p>Authentication is handled by the User Management Service. This service reads
 * shared Redis sessions. Unauthenticated users are redirected to the auth UI.
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-064
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${blog.auth-login-url:/auth/login}")
    private String authLoginUrl;

    /**
     * Single filter chain: session-based auth from shared Redis, public read access.
     */
    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // Public: static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
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
                        .requestMatchers("/admin/audit-logs")
                        .hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/posts/new", "/posts/*/edit", "/posts/*/preview")
                        .authenticated()
                        // Public: blog reading (GET only)
                        .requestMatchers(HttpMethod.GET, "/")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/{slug}")
                        .permitAll()
                        // Public: legal pages
                        .requestMatchers(HttpMethod.GET, "/impressum", "/privacy")
                        .permitAll()
                        // Everything else requires authentication (default-deny)
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(401, "Unauthorized");
                    } else {
                        response.sendRedirect(authLoginUrl);
                    }
                }))
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
}
