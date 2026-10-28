package com.rogerio.wallet_api.config.security;

import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.exception.UserBlockedException;
import com.rogerio.wallet_api.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenService tokenService;
  private final UserRepository userRepository;
  private final HandlerExceptionResolver resolver;

  public TokenAuthenticationFilter(TokenService tokenService,
                                   UserRepository userRepository,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.tokenService = tokenService;
    this.userRepository = userRepository;
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader(AUTHORIZATION_HEADER);

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length()).trim();

      if (!token.isBlank() && tokenService.isTokenValid(token)) {
        String userName = tokenService.extractUserEmail(token);
        Optional<User> optionalUser = userRepository.findByEmail(userName);

        if (optionalUser.isPresent()) {
          User user = optionalUser.get();

          if (user.getStatus() == Status.BLOCKED) {
            resolver.resolveException(request, response, null,
                new UserBlockedException("User is blocked"));
            return;
          }

          List<SimpleGrantedAuthority> authorities =
              Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));

          SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken(userName, null, authorities));
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}