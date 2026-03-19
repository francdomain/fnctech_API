package com.fintech.service;

import com.fintech.dto.Dto.*;
import com.fintech.entity.*;
import com.fintech.exception.FintechException;
import com.fintech.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountResponse createAccount(String email, CreateAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new FintechException("User not found"));

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .user(user)
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .build();

        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> getUserAccounts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new FintechException("User not found"));
        return accountRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getAccount(String accountNumber, String email) {
        Account account = findAccountByNumber(accountNumber);
        validateOwnership(account, email);
        return toResponse(account);
    }

    public Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new FintechException("Account not found: " + accountNumber));
    }

    public void validateOwnership(Account account, String email) {
        if (!account.getUser().getEmail().equals(email)) {
            throw new FintechException("Access denied to this account");
        }
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = "FT" + String.format("%010d", random.nextLong(10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
