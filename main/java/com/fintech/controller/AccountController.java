package com.fintech.controller;

import com.fintech.dto.Dto.*;
import com.fintech.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/accounts
     * Create a new bank account for the authenticated user
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.createAccount(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(account, "Account created successfully"));
    }

    /**
     * GET /api/accounts
     * Get all accounts belonging to the authenticated user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<AccountResponse> accounts = accountService.getUserAccounts(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(accounts, "Accounts fetched"));
    }

    /**
     * GET /api/accounts/{accountNumber}
     * Get a specific account by account number
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse account = accountService.getAccount(accountNumber, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(account, "Account fetched"));
    }
}
