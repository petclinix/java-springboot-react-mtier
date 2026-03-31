package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.web.dto.AdminUserResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class AdminUsersController {

    private final UserService userService;

    public AdminUsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAll() {
        var users = userService.findAll().stream()
                .map(AdminUsersController::toAdminUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AdminUserResponse> deactivate(@PathVariable Long id) {
        var user = userService.deactivate(id);
        return ResponseEntity.ok(toAdminUserResponse(user));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AdminUserResponse> activate(@PathVariable Long id) {
        var user = userService.activate(id);
        return ResponseEntity.ok(toAdminUserResponse(user));
    }

    private static AdminUserResponse toAdminUserResponse(DomainUser user) {
        return new AdminUserResponse(
                user.id(),
                user.username(),
                user.userType().name(),
                user.active());
    }


}
