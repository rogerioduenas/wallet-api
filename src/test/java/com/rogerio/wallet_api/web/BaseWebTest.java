package com.rogerio.wallet_api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rogerio.wallet_api.config.security.DelegatedAccessDeniedHandler;
import com.rogerio.wallet_api.config.security.DelegatedAuthenticationEntryPoint;
import com.rogerio.wallet_api.config.security.SecurityConfig;
import com.rogerio.wallet_api.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
    SecurityConfig.class,
    DelegatedAuthenticationEntryPoint.class,
    DelegatedAccessDeniedHandler.class
})
@AutoConfigureMockMvc
@ImportAutoConfiguration(ObjectMapper.class)
public abstract class BaseWebTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @MockitoBean
  protected TokenService tokenService;
}
