package com.fintech.service;

import com.fintech.dto.Dto.*;
import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import com.fintech.entity.User;
import com.fintech.exception.FintechException;
import com.fintech.repository.AccountRepository;
import com.fintech.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountService accountService;

    @InjectMocks private TransactionService transactionService;

    private User user;
    private Account account;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .fullName("John Doe")
                .password("encoded")
                .phoneNumber("1234567890")
                .build();

        account = Account.builder()
                .id(1L)
                .accountNumber("FT0000000001")
                .user(user)
                .balance(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

        transaction = Transaction.builder()
                .id(1L)
                .referenceNumber("TXN123ABC")
                .amount(BigDecimal.valueOf(200))
                .currency("USD")
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .receiverAccount(account)
                .build();
    }

    // ─── Deposit ─────────────────────────────────────────────────────────────

    @Test
    void deposit_success() {
        DepositRequest request = new DepositRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(200));
        request.setDescription("Salary");

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse response = transactionService.deposit("john@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    void deposit_inactiveAccount_throwsException() {
        account.setStatus(Account.AccountStatus.SUSPENDED);
        DepositRequest request = new DepositRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(200));

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");

        assertThatThrownBy(() -> transactionService.deposit("john@example.com", request))
                .isInstanceOf(FintechException.class)
                .hasMessageContaining("Account is not active");
    }

    // ─── Withdraw ────────────────────────────────────────────────────────────

    @Test
    void withdraw_success() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(300));
        request.setDescription("Bills");

        Transaction withdrawTx = Transaction.builder()
                .id(2L).referenceNumber("TXN456DEF")
                .amount(BigDecimal.valueOf(300)).currency("USD")
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .senderAccount(account).build();

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawTx);

        TransactionResponse response = transactionService.withdraw("john@example.com", request);

        assertThat(response.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(5000));

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");

        assertThatThrownBy(() -> transactionService.withdraw("john@example.com", request))
                .isInstanceOf(FintechException.class)
                .hasMessage("Insufficient funds");
    }

    // ─── Transfer ────────────────────────────────────────────────────────────

    @Test
    void transfer_success() {
        Account receiver = Account.builder()
                .id(2L).accountNumber("FT0000000002")
                .user(User.builder().email("jane@example.com").build())
                .balance(BigDecimal.valueOf(500)).currency("USD")
                .status(Account.AccountStatus.ACTIVE).build();

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FT0000000001");
        request.setToAccountNumber("FT0000000002");
        request.setAmount(BigDecimal.valueOf(400));
        request.setDescription("Rent");

        Transaction transferTx = Transaction.builder()
                .id(3L).referenceNumber("TXN789GHI")
                .amount(BigDecimal.valueOf(400)).currency("USD")
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .senderAccount(account).receiverAccount(receiver).build();

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        when(accountService.findAccountByNumber("FT0000000002")).thenReturn(receiver);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transferTx);

        TransactionResponse response = transactionService.transfer("john@example.com", request);

        assertThat(response.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(receiver.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
    }

    @Test
    void transfer_sameAccount_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FT0000000001");
        request.setToAccountNumber("FT0000000001");
        request.setAmount(BigDecimal.valueOf(100));

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");

        assertThatThrownBy(() -> transactionService.transfer("john@example.com", request))
                .isInstanceOf(FintechException.class)
                .hasMessage("Cannot transfer to same account");
    }

    @Test
    void transfer_insufficientFunds_throwsException() {
        Account receiver = Account.builder()
                .id(2L).accountNumber("FT0000000002")
                .user(User.builder().email("jane@example.com").build())
                .balance(BigDecimal.valueOf(500)).currency("USD")
                .status(Account.AccountStatus.ACTIVE).build();

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("FT0000000001");
        request.setToAccountNumber("FT0000000002");
        request.setAmount(BigDecimal.valueOf(9999));

        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        when(accountService.findAccountByNumber("FT0000000002")).thenReturn(receiver);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");

        assertThatThrownBy(() -> transactionService.transfer("john@example.com", request))
                .isInstanceOf(FintechException.class)
                .hasMessage("Insufficient funds");
    }

    // ─── History ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_success() {
        when(accountService.findAccountByNumber("FT0000000001")).thenReturn(account);
        doNothing().when(accountService).validateOwnership(account, "john@example.com");
        when(transactionRepository.findAllByAccount(account)).thenReturn(List.of(transaction));

        List<TransactionResponse> history = transactionService.getHistory("john@example.com", "FT0000000001");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getReferenceNumber()).isEqualTo("TXN123ABC");
    }
}
