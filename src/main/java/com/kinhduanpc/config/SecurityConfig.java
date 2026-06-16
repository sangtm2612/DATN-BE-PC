package com.kinhduanpc.config;

import com.kinhduanpc.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login",
                    "/auth/refresh", "/auth/forgot-password", "/auth/reset-password",
                    "/auth/verify-email", "/auth/resend-otp").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/products/**", "/categories/**", "/brands/**",
                    "/banners/**", "/stores/**", "/blog/**",
                    "/search/**", "/home/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/orders/track").permitAll()
                .requestMatchers(HttpMethod.GET, "/warranties/lookup").permitAll()
                .requestMatchers("/files/**").permitAll()
                // Admin-only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
                .requestMatchers("/promotions/**").hasRole("ADMIN")
                .requestMatchers("/vouchers/**").hasRole("ADMIN")
                // Authenticated users
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:3001", "http://localhost:5173", "https://*.kinhduanpc.vn"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
