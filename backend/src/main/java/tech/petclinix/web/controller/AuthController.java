package tech.petclinix.web.controller;

import tech.petclinix.logic.domain.Username;
import tech.petclinix.web.dto.LoginRequest;
import tech.petclinix.web.dto.LoginResponse;
import tech.petclinix.security.jwt.JwtUtil;
import tech.petclinix.logic.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = userService.authenticate(new Username(request.username()), request.password());
        var token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
