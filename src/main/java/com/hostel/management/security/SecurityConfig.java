package com.hostel.management.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ===== AUTH =====
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/change-email", "/api/auth/change-password").authenticated()

                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers("/api/settings").permitAll()  // ✅ AJOUTÉ
                        .requestMatchers("/api/public/**").permitAll()

                        // ===== ROOMS PUBLICS (consultation) =====
                        .requestMatchers("/api/rooms").permitAll()
                        .requestMatchers("/api/rooms/{id}").permitAll()
                        .requestMatchers("/api/rooms/available").permitAll()
                        .requestMatchers("/api/rooms/{id}/availability").permitAll()

                        // ===== ROOMS ADMIN (modification) =====
                        .requestMatchers("/api/rooms/upload-photo").authenticated()
                        .requestMatchers("/api/rooms/delete-photo").authenticated()
                        .requestMatchers("/api/rooms/create-with-urls").authenticated()

                        // ===== SERVICES & PACKS =====
                        .requestMatchers("/api/services/**", "/api/packs/**").permitAll()

                        // ===== AVAILABILITY =====
                        .requestMatchers("/api/availability/**").permitAll()

                        // ===== BOOKINGS PUBLICS =====
                        .requestMatchers("/api/bookings").permitAll()
                        .requestMatchers("/api/bookings/reference/**").permitAll()

                        // ===== ADMIN =====
                        .requestMatchers("/api/admin/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
