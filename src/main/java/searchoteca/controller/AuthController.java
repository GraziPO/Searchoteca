package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchoteca.DTO.LoginRequest;
import searchoteca.DTO.LoginResponse;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.JWTService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager, JWTService jwtService, AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        auditLogService.recordLogin(userDetails.getUsername());

        return ResponseEntity.ok(new LoginResponse(token, userDetails.getUsername(), userDetails.getRoleCode()));
    }
}
