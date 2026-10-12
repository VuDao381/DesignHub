package com.designhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.designhub.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/favicon.ico",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/api/auth/register",
                                                                "/api/auth/login")
                                                .permitAll()
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/designs/**",
                                                                "/api/categories/**")
                                                .permitAll()
                                                .requestMatchers("/api/users/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/categories/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/designs/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/orders",
                                                                "/api/orders/user/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/carts/user/**",
                                                                "/api/cart-items",
                                                                "/api/cart-items/cart/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/carts")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/orders")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/orders/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/orders/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/payments")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/design-files")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/reviews",
                                                                "/api/reviews/user/**",
                                                                "/api/favorites",
                                                                "/api/favorites/user/**",
                                                                "/api/download-history",
                                                                "/api/download-history/user/**")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
