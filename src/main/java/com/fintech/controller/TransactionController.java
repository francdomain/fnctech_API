package com.fintech.controller;

import com.fintech.dto.Dto.*;
import com.fintech.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions/deposit
     * Deposit funds into an account
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse tx = transactionService.deposit(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(tx, "Deposit successful"));
    }

    /**
     * POST /api/transactions/withdraw
     * Withdraw funds from an account
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawRequest request) {
        TransactionResponse tx = transactionService.withdraw(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(tx, "Withdrawal successful"));
    }

    /**
     * POST /api/transactions/transfer
     * Transfer funds between two accounts
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse tx = transactionService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(tx, "Transfer successful"));
    }

    /**
     * GET /api/transactions/history/{accountNumber}
     * Get transaction history for a specific account
     */
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String accountNumber) {
        List<TransactionResponse> history = transactionService.getHistory(userDetails.getUsername(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success(history, "Transaction history fetched"));
    }
}
