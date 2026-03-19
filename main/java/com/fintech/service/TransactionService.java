package com.fintech.service;

import com.fintech.dto.Dto.*;
import com.fintech.entity.*;
import com.fintech.exception.FintechException;
import com.fintech.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Transactional
    public TransactionResponse deposit(String email, DepositRequest request) {
        Account account = accountService.findAccountByNumber(request.getAccountNumber());
        accountService.validateOwnership(account, email);
        validateActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .referenceNumber(generateRef())
                .receiverAccount(account)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(LocalDateTime.now())
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse withdraw(String email, WithdrawRequest request) {
        Account account = accountService.findAccountByNumber(request.getAccountNumber());
        accountService.validateOwnership(account, email);
        validateActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new FintechException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .referenceNumber(generateRef())
                .senderAccount(account)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(LocalDateTime.now())
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse transfer(String email, TransferRequest request) {
        Account sender = accountService.findAccountByNumber(request.getFromAccountNumber());
        accountService.validateOwnership(sender, email);
        validateActive(sender);

        Account receiver = accountService.findAccountByNumber(request.getToAccountNumber());
        validateActive(receiver);

        if (sender.getAccountNumber().equals(receiver.getAccountNumber())) {
            throw new FintechException("Cannot transfer to same account");
        }
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new FintechException("Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(request.getAmount()));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction tx = Transaction.builder()
                .referenceNumber(generateRef())
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(request.getAmount())
                .currency(sender.getCurrency())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(LocalDateTime.now())
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    public List<TransactionResponse> getHistory(String email, String accountNumber) {
        Account account = accountService.findAccountByNumber(accountNumber);
        accountService.validateOwnership(account, email);
        return transactionRepository.findAllByAccount(account).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new FintechException("Account is not active: " + account.getAccountNumber());
        }
    }

    private String generateRef() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceNumber(tx.getReferenceNumber())
                .senderAccount(tx.getSenderAccount() != null ? tx.getSenderAccount().getAccountNumber() : null)
                .receiverAccount(tx.getReceiverAccount() != null ? tx.getReceiverAccount().getAccountNumber() : null)
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
