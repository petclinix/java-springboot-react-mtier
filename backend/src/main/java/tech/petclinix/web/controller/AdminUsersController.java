package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.web.controller.mapper.DtoMapper;
import tech.petclinix.web.dto.AdminUserResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserService userService;

    public AdminUsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAll() {
        var users = userService.findAll().stream()
                .map(DtoMapper::toAdminUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AdminUserResponse> deactivate(Authentication authentication, @PathVariable Long id) {
        var user = userService.deactivate(new Username(authentication.getName()), id);
        return ResponseEntity.ok(DtoMapper.toAdminUserResponse(user));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AdminUserResponse> activate(Authentication authentication, @PathVariable Long id) {
        var user = userService.activate(new Username(authentication.getName()), id);
        return ResponseEntity.ok(DtoMapper.toAdminUserResponse(user));
    }

}
