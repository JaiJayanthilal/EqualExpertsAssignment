package org.designprinciple;

import org.designprinciple.ResilienceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceConfigTest {

    @Test
    void shouldCreateCircuitBreaker() {
        assertNotNull(ResilienceConfig.createCircuitBreaker());
    }

    @Test
    void shouldCreateRetry() {
        assertNotNull(ResilienceConfig.createRetry());
    }
}
