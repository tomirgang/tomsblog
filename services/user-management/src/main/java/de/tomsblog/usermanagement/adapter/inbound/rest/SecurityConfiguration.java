package de.tomsblog.usermanagement.adapter.inbound.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the user-management service.
 *
 * <p>Protects all REST endpoints with API-key authentication for service-to-service communication.
 * Actuator health endpoints remain public for Kubernetes probes.
 *
 * @req SWR-043
 * @req SWR-044
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${service.api-key:#{null}}")
    private String apiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: actuator health for K8s probes
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(401, "Unauthorized")))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
