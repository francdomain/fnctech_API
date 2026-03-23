package com.fintech.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void corsConfigurationSourceBeanLoads() {
        assertThat(corsConfigurationSource).isNotNull();
    }
}
