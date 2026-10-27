package com.rogerio.wallet_api.config.security;

import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
  private final DelegatedAccessDeniedHandler delegatedAccessDeniedHandler;

  public SecurityConfig(DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint, DelegatedAccessDeniedHandler delegatedAccessDeniedHandler) {
    this.delegatedAuthenticationEntryPoint = delegatedAuthenticationEntryPoint;
    this.delegatedAccessDeniedHandler = delegatedAccessDeniedHandler;
  }

  @Bean
  public TokenAuthenticationFilter tokenAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
    return new TokenAuthenticationFilter(tokenService,  userRepository);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/v1/customers/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/admins/register").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/v1/admins/users").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/admins/users/{id}/status").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(delegatedAuthenticationEntryPoint)
            .accessDeniedHandler(delegatedAccessDeniedHandler)
        )
        .addFilterBefore(
            tokenAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        )
        .build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}