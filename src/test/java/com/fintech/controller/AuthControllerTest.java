package com.fintech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.dto.Dto.*;
import com.fintech.security.JwtUtils;
import com.fintech.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsService userDetailsService;

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .token("jwt-token")
                .email("john@example.com")
                .fullName("John Doe")
                .role("USER")
                .build();
    }

    @Test
    void register_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("1234567890");

        when(authService.register(any())).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void login_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }
}
