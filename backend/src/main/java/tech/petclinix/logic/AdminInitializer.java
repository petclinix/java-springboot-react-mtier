package tech.petclinix.logic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tech.petclinix.logic.service.UserService;

@Component
public class AdminInitializer implements ApplicationRunner {

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
            System.out.println("AdminInitializer: no admin.username provided — skipping.");
            return;
        }

        userService.findByUsername(adminUsername)
                .ifPresentOrElse(
                        u -> System.out.println("Admin user already exists: " + adminUsername),
                        () -> {
                            System.out.println("Creating initial admin user: " + adminUsername);
                            userService.register(adminUsername, adminPassword);
                        }
                );
    }
}
