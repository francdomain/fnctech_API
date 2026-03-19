package com.fintech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.dto.Dto.*;
import com.fintech.entity.Transaction;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthFilter.class})
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AccountService accountService;
    @MockBean private AuthService authService;
    @MockBean private TransactionService transactionService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsService userDetailsService;

    private TransactionResponse sampleTxResponse(Transaction.TransactionType type) {
        TransactionResponse r = new TransactionResponse();
        r.setId(1L);
        r.setReferenceNumber("TXN123ABC");
        r.setAmount(BigDecimal.valueOf(200));
        r.setCurrency("USD");
        r.setType(type);
        r.setStatus(Transaction.TransactionStatus.COMPLETED);
        return r;
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void deposit_returns200() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(200));

        when(transactionService.deposit(eq("john@example.com"), any()))
                .thenReturn(sampleTxResponse(Transaction.TransactionType.DEPOSIT));

        mockMvc.perform(post("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deposit successful"))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void withdraw_returns200() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(200));

        when(transactionService.withdraw(eq("john@example.com"), any()))
                .thenReturn(sampleTxResponse(Transaction.TransactionType.WITHDRAWAL));

        mockMvc.perform(post("/api/transactions/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Withdrawal successful"))
                .andExpect(jsonPath("$.data.type").value("WITHDRAWAL"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void transfer_returns200() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FT0000000001");
        request.setToAccountNumber("FT0000000002");
        request.setAmount(BigDecimal.valueOf(200));

        when(transactionService.transfer(eq("john@example.com"), any()))
                .thenReturn(sampleTxResponse(Transaction.TransactionType.TRANSFER));

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void getHistory_returns200() throws Exception {
        when(transactionService.getHistory("john@example.com", "FT0000000001"))
                .thenReturn(List.of(sampleTxResponse(Transaction.TransactionType.DEPOSIT)));

        mockMvc.perform(get("/api/transactions/history/FT0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction history fetched"))
                .andExpect(jsonPath("$.data[0].referenceNumber").value("TXN123ABC"));
    }
}
