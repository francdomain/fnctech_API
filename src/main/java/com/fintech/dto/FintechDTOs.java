package com.fintech.dto;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

class RegisterRequest {
    @NotBlank public String fullName;
    @Email @NotBlank public String email;
    @NotBlank @Size(min = 8) public String password;
    @NotBlank public String phoneNumber;
}

class LoginRequest {
    @Email @NotBlank public String email;
    @NotBlank public String password;
}

class AuthResponse {
    public String token;
    public String email;
    public String fullName;
    public String role;
}

// ─── Account DTOs ─────────────────────────────────────────────────────────────

class CreateAccountRequest {
    @NotNull public Account.AccountType accountType;
    @NotBlank public String currency;
}

class AccountResponse {
    public Long id;
    public String accountNumber;
    public Account.AccountType accountType;
    public BigDecimal balance;
    public String currency;
    public Account.AccountStatus status;
    public LocalDateTime createdAt;
}

// ─── Transaction DTOs ─────────────────────────────────────────────────────────

class DepositRequest {
    @NotBlank public String accountNumber;
    @NotNull @DecimalMin("0.01") public BigDecimal amount;
    public String description;
}

class WithdrawRequest {
    @NotBlank public String accountNumber;
    @NotNull @DecimalMin("0.01") public BigDecimal amount;
    public String description;
}

class TransferRequest {
    @NotBlank public String fromAccountNumber;
    @NotBlank public String toAccountNumber;
    @NotNull @DecimalMin("0.01") public BigDecimal amount;
    public String description;
}

class TransactionResponse {
    public Long id;
    public String referenceNumber;
    public String senderAccount;
    public String receiverAccount;
    public BigDecimal amount;
    public String currency;
    public Transaction.TransactionType type;
    public Transaction.TransactionStatus status;
    public String description;
    public LocalDateTime createdAt;
    public LocalDateTime completedAt;
}
