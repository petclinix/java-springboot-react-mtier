package tech.petclinix.web.controller;

import tech.petclinix.logic.service.UserService;
import tech.petclinix.web.dto.RegisterRequest;
import tech.petclinix.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        if (userService.existsByUsername(request.username())) {
            return ResponseEntity.status(409)
                    .body("Username already taken");
        }
        var user = userService.register(request.username(), request.password());

        return ResponseEntity.ok(new UserResponse(user.id(), user.username()));
    }
}
