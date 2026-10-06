package com.asfaw.pastebin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // pastes stay anonymous-friendly; only personal pages require login
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/mine", "/p/*/edit", "/p/*/delete").authenticated()
                        .anyRequest().permitAll())
                // the JSON API is token/stateless territory, not browser forms
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }
}
