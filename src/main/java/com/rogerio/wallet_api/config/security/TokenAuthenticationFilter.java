package com.rogerio.wallet_api.config.security;

import com.rogerio.wallet_api.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenService tokenService;

  public TokenAuthenticationFilter(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader(AUTHORIZATION_HEADER);

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length()).trim();

      if (!token.isBlank() && tokenService.isTokenValid(token)) {
        String userName = tokenService.extractUsername(token);
        String role = tokenService.extractRole(token);

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(role)
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userName,
                null,
                authorities
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}


