package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.model.UserModel;
import searchoteca.service.UserService;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> findById(@PathVariable String username) {
        UserModel user = userService.findByUsername(username);

        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserModel user){
        return ResponseEntity.ok(userService.create(user));
    }

    @PutMapping("/{username}")
    public ResponseEntity<?> update(@PathVariable String username, @RequestBody UserModel userInfo){
        return ResponseEntity.ok(userService.update(username, userInfo));
    }

    @PatchMapping("/{username}/status/ativar")
    public ResponseEntity<?> activate (@PathVariable String username){
        userService.activate(username);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{username}/status/desativar")
    public ResponseEntity<?> deactivate (@PathVariable String username){
        userService.deactivate(username);
        return ResponseEntity.ok().build();
    }
}
