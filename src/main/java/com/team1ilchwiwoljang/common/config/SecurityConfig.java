package com.team1ilchwiwoljang.common.config;

import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.JwtAuthenticationFilter;
import com.team1ilchwiwoljang.common.security.SecurityErrorResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseHandler securityErrorResponseHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);

        // Spring Boot가 일반 Servlet Filter로 자동 등록을 방지합니다.
        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 서버가 로그인 상태를 세션에 저장하지 않도록 설정합니다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Security 단계에서 발생한 인증/인가 실패 응답을 공통 ErrorResponse 형식으로 내려줍니다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseHandler.writeErrorResponse(response, ErrorCode.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorResponseHandler.writeErrorResponse(response, ErrorCode.FORBIDDEN)
                        )
                )

                // 인가 설정
                .authorizeHttpRequests(this::configureAuthorization)

                // Controller에 도착하기 전에 JWT를 먼저 검증합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",

            /*
             * STOMP WebSocket handshake endpoint입니다.
             * HTTP Security에서는 통과시키고,
             * 실제 JWT 인증은 STOMP CONNECT 프레임에서 처리합니다.
             */
            "/ws/chat",

            /*
             * 채팅 테스트 화면은 local 프로파일에서만 Controller가 제공합니다.
             * 실제 STOMP 인증은 /ws/chat 연결 이후 CONNECT 프레임에서 처리합니다.
             */
            "/chat-test.html",

            // Spring 내부 에러 경로
            "/error",
            "/error/**"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/categories",
            "/api/categories/**",
            "/api/products",
            "/api/products/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/admins/**"
    };

    private void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                .anyRequest().authenticated();
    }
}
