package com.fintech.service;

import com.fintech.dto.Dto.*;
import com.fintech.entity.Account;
import com.fintech.entity.User;
import com.fintech.exception.FintechException;
import com.fintech.repository.AccountRepository;
import com.fintech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AccountService accountService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encoded")
                .phoneNumber("1234567890")
                .build();

        account = Account.builder()
                .id(1L)
                .accountNumber("FT0000000001")
                .user(user)
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createAccount_success() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);
        request.setCurrency("USD");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.createAccount("john@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("FT0000000001");
        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    void createAccount_userNotFound_throwsException() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);
        request.setCurrency("USD");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount("nobody@example.com", request))
                .isInstanceOf(FintechException.class)
                .hasMessage("User not found");
    }

    @Test
    void getUserAccounts_returnsList() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(List.of(account));

        List<AccountResponse> accounts = accountService.getUserAccounts("john@example.com");

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("FT0000000001");
    }

    @Test
    void getUserAccounts_userNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getUserAccounts("nobody@example.com"))
                .isInstanceOf(FintechException.class)
                .hasMessage("User not found");
    }

    @Test
    void getAccount_success() {
        when(accountRepository.findByAccountNumber("FT0000000001")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount("FT0000000001", "john@example.com");

        assertThat(response.getAccountNumber()).isEqualTo("FT0000000001");
    }

    @Test
    void getAccount_wrongOwner_throwsException() {
        when(accountRepository.findByAccountNumber("FT0000000001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccount("FT0000000001", "other@example.com"))
                .isInstanceOf(FintechException.class)
                .hasMessage("Access denied to this account");
    }

    @Test
    void findAccountByNumber_notFound_throwsException() {
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountByNumber("INVALID"))
                .isInstanceOf(FintechException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void validateOwnership_wrongEmail_throwsException() {
        assertThatThrownBy(() -> accountService.validateOwnership(account, "wrong@example.com"))
                .isInstanceOf(FintechException.class)
                .hasMessage("Access denied to this account");
    }

    @Test
    void toResponse_mapsAllFields() {
        AccountResponse response = accountService.toResponse(account);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountNumber()).isEqualTo("FT0000000001");
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
        assertThat(response.getAccountType()).isEqualTo(Account.AccountType.SAVINGS);
    }
}
