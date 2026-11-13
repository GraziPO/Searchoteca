package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.ChangePasswordRequest;
import searchoteca.DTO.users.UserResponse;
import searchoteca.DTO.users.student.StudentRequest;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.UserModel;
import searchoteca.resetPassword.PasswordChangeService;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.users.StudentService;
import searchoteca.service.users.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {
    private final UserService userService;
    private final StudentService studentService;
    private final AuditLogService auditLogService;
    private final PasswordChangeService passwordChangeService;

    public UserController(UserService userService, AuditLogService auditLogService, StudentService studentService, PasswordChangeService passwordChangeService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.studentService = studentService;
        this.passwordChangeService = passwordChangeService;
    }

    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping
    public ResponseEntity<?> findAll() {
        List<UserResponse> user = userService.findAll().stream().map(UserResponse::from).toList();
        auditLogService.record(AuditAction.ACCESS, "All Users");
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping("/{username}")
    public ResponseEntity<?> findById(@PathVariable String username) {
        UserModel user = userService.findByUsername(username);
        auditLogService.record(AuditAction.ACCESS, "users" + username);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PreAuthorize("hasAuthority('users:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserModel user) {
        UserModel created = userService.create(user);
        auditLogService.record(AuditAction.CREATE, "usuário " + created.getUsername() + " (perfil " + created.getRole_code() + ")");
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasAuthority('users:edit')")
    @PutMapping("/{username}")
    public ResponseEntity<?> update(@PathVariable String username, @RequestBody UserModel userInfo) {
        // o registro (com o antes -> depois) é feito no UserService
        return ResponseEntity.ok(userService.update(username, userInfo));
    }

    //======================= cria usuários do tipo "aluno" =======================

    @PreAuthorize("hasAuthority('students:create')")
    @PostMapping("/aluno")
    public ResponseEntity<?> createStudent(@RequestBody StudentRequest studentInfo) {
        studentService.create(studentInfo);
        auditLogService.record(AuditAction.CREATE, "aluno RA " + studentInfo.ra());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/senha")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        passwordChangeService.changeOwnPassword(user.getUserId(), request.currentPassword(), request.newPassword());
        // RF18: o token usado nesta requisição já não vale; o frontend manda para o login
        return ResponseEntity.ok(Map.of(
                "message", "Senha alterada. Entre novamente com a nova senha.",
                "reloginRequired", true));
    }

    // ---------------- ROTAS DE ATIVAÇÃO / DESATICACAO -------------------

    @PreAuthorize("hasAuthority('users:edit')")
    @PatchMapping("/{username}/status/ativar")
    public ResponseEntity<?> activate(@PathVariable String username) {
        userService.activate(username);
        auditLogService.record(AuditAction.ACTIVATE_USER, "usuário " + username);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('users:edit')")
    @PatchMapping("/{username}/status/desativar")
    public ResponseEntity<?> deactivate(@PathVariable String username) {
        userService.deactivate(username);
        auditLogService.record(AuditAction.DEACTIVATE_USER, "usuário " + username);
        return ResponseEntity.ok().build();
    }
}