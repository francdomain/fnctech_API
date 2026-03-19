package com.fintech.dto;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    @Test
    void registerRequest_settersAndGetters() {
        Dto.RegisterRequest req = new Dto.RegisterRequest();
        req.setFullName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password1!");
        req.setPhoneNumber("+1111111111");

        assertThat(req.getFullName()).isEqualTo("Alice");
        assertThat(req.getEmail()).isEqualTo("alice@example.com");
        assertThat(req.getPassword()).isEqualTo("Password1!");
        assertThat(req.getPhoneNumber()).isEqualTo("+1111111111");
    }

    @Test
    void loginRequest_settersAndGetters() {
        Dto.LoginRequest req = new Dto.LoginRequest();
        req.setEmail("bob@example.com");
        req.setPassword("secret");

        assertThat(req.getEmail()).isEqualTo("bob@example.com");
        assertThat(req.getPassword()).isEqualTo("secret");
    }

    @Test
    void authResponse_builderAndGetters() {
        Dto.AuthResponse resp = Dto.AuthResponse.builder()
                .token("jwt-token")
                .email("alice@example.com")
                .fullName("Alice")
                .role("USER")
                .build();

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");
        assertThat(resp.getFullName()).isEqualTo("Alice");
        assertThat(resp.getRole()).isEqualTo("USER");
    }

    @Test
    void createAccountRequest_settersAndGetters() {
        Dto.CreateAccountRequest req = new Dto.CreateAccountRequest();
        req.setAccountType(Account.AccountType.SAVINGS);
        req.setCurrency("USD");

        assertThat(req.getAccountType()).isEqualTo(Account.AccountType.SAVINGS);
        assertThat(req.getCurrency()).isEqualTo("USD");
    }

    @Test
    void accountResponse_builderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        Dto.AccountResponse resp = Dto.AccountResponse.builder()
                .id(1L)
                .accountNumber("FT0000000001")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.TEN)
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(now)
                .build();

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getAccountNumber()).isEqualTo("FT0000000001");
        assertThat(resp.getCurrency()).isEqualTo("USD");
        assertThat(resp.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void depositRequest_settersAndGetters() {
        Dto.DepositRequest req = new Dto.DepositRequest();
        req.setAccountNumber("FT0000000001");
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("Deposit");

        assertThat(req.getAccountNumber()).isEqualTo("FT0000000001");
        assertThat(req.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(req.getDescription()).isEqualTo("Deposit");
    }

    @Test
    void withdrawRequest_settersAndGetters() {
        Dto.WithdrawRequest req = new Dto.WithdrawRequest();
        req.setAccountNumber("FT0000000001");
        req.setAmount(new BigDecimal("50.00"));
        req.setDescription("Withdraw");

        assertThat(req.getAccountNumber()).isEqualTo("FT0000000001");
        assertThat(req.getAmount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void transferRequest_settersAndGetters() {
        Dto.TransferRequest req = new Dto.TransferRequest();
        req.setFromAccountNumber("FT0000000001");
        req.setToAccountNumber("FT0000000002");
        req.setAmount(new BigDecimal("200.00"));
        req.setDescription("Transfer");

        assertThat(req.getFromAccountNumber()).isEqualTo("FT0000000001");
        assertThat(req.getToAccountNumber()).isEqualTo("FT0000000002");
        assertThat(req.getAmount()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    void transactionResponse_builderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        Dto.TransactionResponse resp = Dto.TransactionResponse.builder()
                .id(1L)
                .referenceNumber("REF001")
                .senderAccount("FT0000000001")
                .receiverAccount("FT0000000002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Test transfer")
                .createdAt(now)
                .completedAt(now)
                .build();

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getReferenceNumber()).isEqualTo("REF001");
        assertThat(resp.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);
        assertThat(resp.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
    }

    @Test
    void apiResponse_okFactory() {
        Dto.ApiResponse<String> resp = Dto.ApiResponse.ok("data", "Success");

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("Success");
        assertThat(resp.getData()).isEqualTo("data");
    }

    @Test
    void apiResponse_errorFactory() {
        Dto.ApiResponse<Void> resp = Dto.ApiResponse.error("Something went wrong");

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("Something went wrong");
        assertThat(resp.getData()).isNull();
    }

    @Test
    void apiResponse_noArgsConstructorAndSetters() {
        Dto.ApiResponse<String> resp = new Dto.ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("msg");
        resp.setData("value");

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("msg");
        assertThat(resp.getData()).isEqualTo("value");
    }
}
