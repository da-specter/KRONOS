package com.etapa_productiva.kronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // 🔓 Puertas abiertas para controlar el flujo por código
            )
            .formLogin(form -> form.disable()) 
            .httpBasic(basic -> basic.disable()); 
            
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Al proveer este usuario fantasma en memoria, Spring Boot DEJA de generar claves aleatorias
        UserDetails userFantasma = User.withUsername("voidUser")
                .password("{noop}voidPass")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(userFantasma);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
