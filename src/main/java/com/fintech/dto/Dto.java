package com.fintech.dto;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dto {

    private Dto() {}

    // ─── Auth ────────────────────────────────────────────────────────────────

    @Data public static class RegisterRequest {
        @NotBlank private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") private String password;
        @NotBlank private String phoneNumber;
    }

    @Data public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data @Builder public static class AuthResponse {
        private String token;
        private String email;
        private String fullName;
        private String role;
    }

    // ─── Account ─────────────────────────────────────────────────────────────

    @Data public static class CreateAccountRequest {
        @NotNull private Account.AccountType accountType;
        @NotBlank private String currency;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor public static class AccountResponse {
        private Long id;
        private String accountNumber;
        private Account.AccountType accountType;
        private BigDecimal balance;
        private String currency;
        private Account.AccountStatus status;
        private LocalDateTime createdAt;
    }

    // ─── Transaction ─────────────────────────────────────────────────────────

    @Data public static class DepositRequest {
        @NotBlank private String accountNumber;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private String description;
    }

    @Data public static class WithdrawRequest {
        @NotBlank private String accountNumber;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private String description;
    }

    @Data public static class TransferRequest {
        @NotBlank private String fromAccountNumber;
        @NotBlank private String toAccountNumber;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private String description;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor public static class TransactionResponse {
        private Long id;
        private String referenceNumber;
        private String senderAccount;
        private String receiverAccount;
        private BigDecimal amount;
        private String currency;
        private Transaction.TransactionType type;
        private Transaction.TransactionStatus status;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    // ─── Generic ─────────────────────────────────────────────────────────────

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data, String message) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
