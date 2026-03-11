package com.fintech.dto;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dto {

    // ─── Auth ────────────────────────────────────────────────────────────────

    @Data public static class RegisterRequest {
        @NotBlank public String fullName;
        @Email @NotBlank public String email;
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") public String password;
        @NotBlank public String phoneNumber;
    }

    @Data public static class LoginRequest {
        @Email @NotBlank public String email;
        @NotBlank public String password;
    }

    @Data @Builder public static class AuthResponse {
        public String token;
        public String email;
        public String fullName;
        public String role;
    }

    // ─── Account ─────────────────────────────────────────────────────────────

    @Data public static class CreateAccountRequest {
        @NotNull public Account.AccountType accountType;
        @NotBlank public String currency;
    }

    @Data @Builder public static class AccountResponse {
        public Long id;
        public String accountNumber;
        public Account.AccountType accountType;
        public BigDecimal balance;
        public String currency;
        public Account.AccountStatus status;
        public LocalDateTime createdAt;
    }

    // ─── Transaction ─────────────────────────────────────────────────────────

    @Data public static class DepositRequest {
        @NotBlank public String accountNumber;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
        public String description;
    }

    @Data public static class WithdrawRequest {
        @NotBlank public String accountNumber;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
        public String description;
    }

    @Data public static class TransferRequest {
        @NotBlank public String fromAccountNumber;
        @NotBlank public String toAccountNumber;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
        public String description;
    }

    @Data @Builder public static class TransactionResponse {
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

    // ─── Generic ─────────────────────────────────────────────────────────────

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ApiResponse<T> {
        public boolean success;
        public String message;
        public T data;

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
