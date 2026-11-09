package com.seniorlink.api.config;
import com.seniorlink.api.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = JwtService.normalizarRol(jwt.getClaimAsString("rol"));
            return rol.isBlank() ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return converter;
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/instituciones-aliadas").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/mercadopago/webhook").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/total").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${seniorlink.frontend.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
