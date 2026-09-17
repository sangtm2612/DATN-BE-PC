package com.kinhduanpc.config;

import com.kinhduanpc.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
                    "/products/**", "/categories/**", "/brands/**", "/tags/**",
                    "/banners/**", "/blog/**",
                    "/search/**", "/home/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/orders/track").permitAll()
                .requestMatchers(HttpMethod.POST, "/orders").permitAll()  // Guest checkout
                .requestMatchers(HttpMethod.GET, "/warranties/lookup").permitAll()
                .requestMatchers("/files/**").permitAll()
                .requestMatchers("/cart/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/build-pc/component-types", "/build-pc/components").permitAll()
                .requestMatchers(HttpMethod.POST, "/build-pc/check-compatibility").permitAll()
                .requestMatchers(HttpMethod.GET, "/promotions/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/shipping-methods").permitAll()
                .requestMatchers(HttpMethod.GET, "/config/**").permitAll()  // Public config endpoints
                // Payment create endpoints: public để guest COD có thể thanh toán cọc
                .requestMatchers(HttpMethod.POST, "/payments/vnpay/create").permitAll()
                .requestMatchers(HttpMethod.POST, "/payments/zalopay/create").permitAll()
                // VNPay callbacks
                .requestMatchers("/payments/vnpay/ipn", "/payments/vnpay/return").permitAll()
                // MoMo callbacks (called by MoMo server — no JWT)
                .requestMatchers(HttpMethod.POST, "/payments/momo/ipn").permitAll()
                .requestMatchers(HttpMethod.GET,  "/payments/momo/return").permitAll()
                // ZaloPay callback + return (callback called by ZaloPay server, return is user browser redirect)
                .requestMatchers(HttpMethod.POST, "/payments/zalopay/callback").permitAll()
                .requestMatchers(HttpMethod.GET,  "/payments/zalopay/return").permitAll()
                // Store stock (noi bo, phai khai bao TRUOC quy tac GET /stores/** permitAll ben duoi)
                .requestMatchers("/stores/*/stock", "/stores/*/stock/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET, "/stores/**").permitAll()
                // Reviews: GET is public so guests can read product reviews
                .requestMatchers(HttpMethod.GET, "/reviews/**").permitAll()
                // Admin dashboard & stats (ADMIN + STAFF — AdminController uses @PreAuthorize for fine-grained control)
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
                .requestMatchers("/promotions/**").hasRole("ADMIN")
                // Vouchers: check & my-vouchers are for authenticated users; everything else is ADMIN-only
                .requestMatchers(HttpMethod.GET, "/vouchers/check", "/vouchers/my-vouchers").authenticated()
                .requestMatchers("/vouchers/**").hasRole("ADMIN")
                .requestMatchers("/shipping-methods/**").hasRole("ADMIN")
                // Authenticated users
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:3001", "http://localhost:5173", "https://*.kinhduanpc.vn", "https://*.onrender.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
