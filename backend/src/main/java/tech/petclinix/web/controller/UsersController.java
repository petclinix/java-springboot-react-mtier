package tech.petclinix.web.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.web.controller.mapper.DtoMapper;
import tech.petclinix.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        try {
            var user = userService.register(new Username(request.username()), request.password(), request.type());
            return ResponseEntity.ok(toUserResponse(user));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("Username already taken");
        }
    }

    @GetMapping("/aboutme")
    public ResponseEntity<?> aboutme(Authentication authentication) {
        var user = userService.findByUsername(new Username(authentication.getName()))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authentication.getName()));

        return ResponseEntity.ok(toUserResponse(user));
    }
}
