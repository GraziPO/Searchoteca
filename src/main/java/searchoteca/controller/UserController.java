package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.users.StudentRequest;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.UserModel;
import searchoteca.service.users.StudentService;
import searchoteca.service.users.UserService;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {
    private final UserService userService;
    private final StudentService studentService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService,  AuditLogService auditLogService, StudentService studentService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.studentService = studentService;
    }

    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping
    public ResponseEntity<?> findAll() {
        auditLogService.record(AuditAction.ACCESS,"All Users");
        return ResponseEntity.ok(userService.findAll());
    }

    @PreAuthorize("hasAuthority('users:view')")
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

    //======================= cria usuários do tipo "aluno" =======================

    @PreAuthorize("hasAuthority('students:create')")
    @PostMapping("/aluno")
    public ResponseEntity<?> createStudent(@RequestBody StudentRequest studentInfo){
        studentService.create(studentInfo);
        auditLogService.record(AuditAction.CREATE,"student" + studentInfo.ra());
        return ResponseEntity.ok().build();
    }
}
