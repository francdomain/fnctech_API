package com.fintech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.dto.Dto.*;
import com.fintech.entity.Account;
import com.fintech.config.SecurityConfig;
import com.fintech.security.JwtAuthFilter;
import com.fintech.security.JwtUtils;
import com.fintech.service.AccountService;
import com.fintech.service.AuthService;
import com.fintech.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AccountService accountService;
    @MockBean private AuthService authService;
    @MockBean private TransactionService transactionService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsService userDetailsService;

    private AccountResponse sampleAccountResponse() {
        AccountResponse r = new AccountResponse();
        r.setId(1L);
        r.setAccountNumber("FT0000000001");
        r.setAccountType(Account.AccountType.SAVINGS);
        r.setBalance(BigDecimal.valueOf(1000));
        r.setCurrency("USD");
        r.setStatus(Account.AccountStatus.ACTIVE);
        return r;
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void createAccount_returns200() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);
        request.setCurrency("USD");

        when(accountService.createAccount(eq("john@example.com"), any())).thenReturn(sampleAccountResponse());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("FT0000000001"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void getMyAccounts_returns200() throws Exception {
        when(accountService.getUserAccounts("john@example.com")).thenReturn(List.of(sampleAccountResponse()));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].accountNumber").value("FT0000000001"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void getAccount_returns200() throws Exception {
        when(accountService.getAccount("FT0000000001", "john@example.com")).thenReturn(sampleAccountResponse());

        mockMvc.perform(get("/api/accounts/FT0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value("FT0000000001"));
    }

    @Test
    void getMyAccounts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isForbidden());
    }
}
