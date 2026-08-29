package tech.petclinix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the application's {@link Clock}. Injecting {@code Clock} instead of calling
 * {@code LocalDateTime.now()} directly lets services that need "now" for business rules
 * (e.g. the appointment cancellation cutoff) be tested deterministically with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
