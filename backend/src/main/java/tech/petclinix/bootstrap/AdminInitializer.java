package tech.petclinix.bootstrap;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.logic.domain.UserType;
import org.slf4j.Logger;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            LOGGER.warn("AdminInitializer: admin.username or admin.password not set — skipping.");
            return;
        }

        Username username = new Username(adminUsername);
        userService.findByUsername(username)
                .ifPresentOrElse(
                        u -> LOGGER.info("Admin user already exists: {}", adminUsername),
                        () -> {
                            LOGGER.info("Creating initial admin user: {}", adminUsername);
                            userService.register(username, adminPassword, UserType.ADMIN);
                        }
                );
    }
}
