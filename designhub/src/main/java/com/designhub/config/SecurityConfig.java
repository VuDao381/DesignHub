package com.designhub.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.designhub.repository.UserRepository;
import com.designhub.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final String allowedOrigins;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}") String allowedOrigins) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.allowedOrigins = allowedOrigins;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(UserRepository userRepository) {
                return identifier -> userRepository.findByEmail(identifier)
                                .or(() -> userRepository.findByUsername(identifier))
                                .map(user -> org.springframework.security.core.userdetails.User.builder()
                                                .username(user.getEmail())
                                                .password(user.getPassword())
                                                .authorities("ROLE_" + user.getRole().name())
                                                .build())
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Không tìm thấy người dùng: " + identifier));
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                String rawOrigins = (allowedOrigins != null && !allowedOrigins.isBlank())
                                ? allowedOrigins
                                : "http://localhost:3000,http://localhost:5173,http://localhost:8080";
                List<String> origins = List.of(StringUtils.tokenizeToStringArray(rawOrigins, ","));
                config.setAllowedOriginPatterns(origins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .formLogin(formLogin -> formLogin.disable())
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/designer",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/favicon.ico",
                                                                "/error",
                                                                "/uploads/images/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/api/auth/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/designs/my",
                                                                "/api/designs/me")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                "/api/designers/me",
                                                                "/api/designers/register")
                                                .authenticated()
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/designs/**",
                                                                "/api/categories/**",
                                                                "/api/design-images/**",
                                                                "/api/designers/**",
                                                                "/api/reviews/design/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/designers")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/designers/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/designers/**")
                                                .authenticated()
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/design-images/**",
                                                                "/api/design-files/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/design-images/**",
                                                                "/api/design-files/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/design-images/**",
                                                                "/api/design-files/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/upload/file")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/upload/image")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(
                                                                "/api/users/me",
                                                                "/api/users/change-password")
                                                .authenticated()
                                                .requestMatchers("/api/users/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/analytics/admin/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/analytics/designer/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers("/api/categories/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/designs/*/status")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/designs/**")
                                                .hasAnyRole("ADMIN", "DESIGNER")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/orders",
                                                                "/api/cart-items")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/carts")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/orders")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/orders/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/orders/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/order-details/**",
                                                                "/api/order-details")
                                                .hasRole("ADMIN")
                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/order-details/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/order-details/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/orders/*/approve")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/payments/*/confirm")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/payments")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/payments/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/design-files/*/download")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/design-files")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/reviews",
                                                                "/api/favorites",
                                                                "/api/download-history")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
