package com.techora;

import com.techora.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationIntegrationIT extends AbstractIntegrationTest {

    @Test
    void contextLoadsWithExternalContainers() {
        assertThat(POSTGRES.isRunning()).isTrue();
        assertThat(REDIS.isRunning()).isTrue();
        assertThat(KAFKA.isRunning()).isTrue();
    }
}
