package com.fintech.config;

import com.fintech.entity.*;
import com.fintech.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Admin user
            User admin = userRepository.save(User.builder()
                    .fullName("Admin User")
                    .email("admin@fintech.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .phoneNumber("+1000000000")
                    .role(User.Role.ADMIN)
                    .build());

            // Regular demo user
            User demo = userRepository.save(User.builder()
                    .fullName("Jane Doe")
                    .email("jane@fintech.com")
                    .password(passwordEncoder.encode("Jane@1234"))
                    .phoneNumber("+1234567890")
                    .build());

            accountRepository.save(Account.builder()
                    .accountNumber("FT0000000001")
                    .user(demo)
                    .accountType(Account.AccountType.SAVINGS)
                    .balance(new BigDecimal("5000.00"))
                    .currency("USD")
                    .build());

            log.info("✅ Demo data seeded:");
            log.info("   Admin  → admin@fintech.com / Admin@1234");
            log.info("   User   → jane@fintech.com  / Jane@1234");
        }
    }
}
