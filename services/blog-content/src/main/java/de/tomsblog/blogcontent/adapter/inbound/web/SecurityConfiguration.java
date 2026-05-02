package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Spring Security configuration for the blog content service.
 *
 * <p>Protects all write operations behind form-based login (web UI) and HTTP Basic (REST API).
 * Public read access remains available for published posts.
 *
 * <p>This is an MVP solution for Phase 2. It will be replaced by the Custom Identity Provider
 * (SWR-016, SWA-010) in Phase 4.
 *
 * @req SWR-028
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                        // Public: Swagger UI and API docs
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                        .permitAll()
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
                .formLogin(form -> form.loginPage("/login")
                        .defaultSuccessUrl("/posts", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/posts").permitAll())
                .httpBasic(basic -> {})
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.builder()
                .username(adminProperties.username())
                .password(passwordEncoder().encode(adminProperties.password()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
