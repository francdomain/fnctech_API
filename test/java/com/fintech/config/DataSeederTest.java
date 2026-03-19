package com.fintech.config;

import com.fintech.entity.Account;
import com.fintech.entity.User;
import com.fintech.repository.AccountRepository;
import com.fintech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataSeeder, "adminPassword", "Admin@1234");
        ReflectionTestUtils.setField(dataSeeder, "demoPassword", "Demo@1234");
    }

    @Test
    void run_whenNoUsers_seedsData() throws Exception {
        User demoUser = User.builder()
                .fullName("Jane Doe")
                .email("jane@fintech.com")
                .build();

        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(demoUser);
        when(accountRepository.save(any(Account.class))).thenReturn(new Account());

        dataSeeder.run();

        verify(userRepository, times(2)).save(any(User.class));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void run_whenUsersExist_skipsSeeding() throws Exception {
        when(userRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }
}
