package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.UserModel;
import searchoteca.service.UserService;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {
    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService,  AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        auditLogService.record(AuditAction.ACCESS,"All Users");
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> findById(@PathVariable String username) {
        auditLogService.record(AuditAction.ACCESS,"users" + username);
        UserModel user = userService.findByUsername(username);

        return ResponseEntity.ok(user);
    }
    @PreAuthorize("hasAuthority('users:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserModel user){
        auditLogService.record(AuditAction.CREATE,"users" + user.getUsername());
        return ResponseEntity.ok(userService.create(user));
    }

    @PreAuthorize("hasAuthority('users:edit')")
    @PutMapping("/{username}")
    public ResponseEntity<?> update(@PathVariable String username, @RequestBody UserModel userInfo){
        auditLogService.record(AuditAction.UPDATE,"users" + username);
        return ResponseEntity.ok(userService.update(username, userInfo));
    }

    @PreAuthorize("hasAuthority('users:edit')")
    @PatchMapping("/{username}/status/ativar")
    public ResponseEntity<?> activate (@PathVariable String username){
        userService.activate(username);
        auditLogService.record(AuditAction.ACTIVATE_USER,"users" + username);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('users:edit')")
    @PatchMapping("/{username}/status/desativar")
    public ResponseEntity<?> deactivate (@PathVariable String username){
        userService.deactivate(username);
        auditLogService.record(AuditAction.DEACTIVATE_USER,"users" + username);
        return ResponseEntity.ok().build();
    }
}
