package tech.petclinix.logic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.logic.domain.UserType;
import java.util.logging.Logger;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final static Logger LOGGER = Logger.getLogger(AdminInitializer.class.getName());

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
        if (adminUsername == null || adminUsername.isBlank()) {
            LOGGER.info("AdminInitializer: no admin.username provided — skipping.");
            return;
        }

        userService.findByUsername(adminUsername)
                .ifPresentOrElse(
                        u -> LOGGER.info("Admin user already exists: " + adminUsername),
                        () -> {
                            LOGGER.info("Creating initial admin user: " + adminUsername);
                            userService.register(adminUsername, adminPassword, UserType.ADMIN);
                        }
                );
    }
}
