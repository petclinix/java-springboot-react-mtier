package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.petclinix.logic.service.UserService;
import tech.petclinix.persistence.mapper.UserMapper;
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
                .map(e -> new AdminUserResponse(
                        e.getId(),
                        e.getUsername(),
                        UserMapper.getUserType(e).name(),
                        e.isActive()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AdminUserResponse> deactivate(@PathVariable Long id) {
        var entity = userService.deactivate(id);
        var response = new AdminUserResponse(
                entity.getId(),
                entity.getUsername(),
                UserMapper.getUserType(entity).name(),
                entity.isActive());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AdminUserResponse> activate(@PathVariable Long id) {
        var entity = userService.activate(id);
        var response = new AdminUserResponse(
                entity.getId(),
                entity.getUsername(),
                UserMapper.getUserType(entity).name(),
                entity.isActive());
        return ResponseEntity.ok(response);
    }
}
