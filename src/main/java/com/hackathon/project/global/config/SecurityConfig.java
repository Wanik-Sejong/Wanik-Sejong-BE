package com.hackathon.project.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 로그인 기능 없으므로 CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 활성화 (아래 Bean 사용)
            .cors(Customizer.withDefaults())

            // 세션 사용 안 함
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 모든 요청 허용 (방어는 다른 레이어에서)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/health"
                ).permitAll()
                .anyRequest().permitAll()
            )

            // 기본 보안 헤더
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(FrameOptionsConfig::sameOrigin)
            );

        return http.build();
    }
}