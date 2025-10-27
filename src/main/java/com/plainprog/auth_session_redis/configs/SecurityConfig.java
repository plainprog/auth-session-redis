package com.plainprog.auth_session_redis.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Updated way to disable CSRF
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/session/validate").authenticated()
                        .requestMatchers(request -> {
                        String remoteAddr = request.getRemoteAddr();
                        return remoteAddr.startsWith("10.")
                                || remoteAddr.startsWith("192.168.")
                                || remoteAddr.startsWith("172.")
                                || remoteAddr.equals("127.0.0.1")
                                || remoteAddr.equals("0:0:0:0:0:0:0:1")
                                || remoteAddr.equals("localhost");
                        })
                        .permitAll()
                        .anyRequest()
                        .denyAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }
}
