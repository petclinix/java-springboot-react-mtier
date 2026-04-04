package tech.petclinix.web.controller;

import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.web.dto.UserResponse;

import static tech.petclinix.web.controller.mapper.DtoMapper.toUserResponse;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (request.type() == UserType.ADMIN) {
            return ResponseEntity.status(403).body("Cannot self-register as ADMIN");
        }
        var user = userService.register(new Username(request.username()), request.password(), request.type());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping("/aboutme")
    public ResponseEntity<UserResponse> aboutme(Authentication authentication) {
        var user = userService.findByUsername(new Username(authentication.getName()))
                .orElseThrow(() -> new NotFoundException("User not found: " + authentication.getName()));

        return ResponseEntity.ok(toUserResponse(user));
    }
}
