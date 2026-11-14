package searchoteca.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.users.account.AccountRequestView;
import searchoteca.DTO.users.account.DeactivationRequest;
import searchoteca.DTO.users.account.DecisionRequest;
import searchoteca.DTO.users.account.ReactivationRequest;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.users.AccountLifecycleService;

import java.util.List;
import java.util.Map;

/** Pedidos de desativação e reativação de conta (RF19). */
@RestController
@RequestMapping("/api")
public class AccountRequestController {

    private final AccountLifecycleService lifecycleService;

    public AccountRequestController(AccountLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    // ------------------------------------------------------------------ aluno logado

    @PreAuthorize("hasAuthority('account:request')")
    @PostMapping("/usuarios/me/desativacao")
    public ResponseEntity<AccountRequestView> requestDeactivation(@RequestBody(required = false) DeactivationRequest body,
                                                                  Authentication auth) {
        String reason = body == null ? null : body.reason();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lifecycleService.requestDeactivation(userId(auth), reason));
    }

    @PreAuthorize("hasAuthority('account:request')")
    @DeleteMapping("/usuarios/me/desativacao")
    public ResponseEntity<Void> cancelDeactivation(Authentication auth) {
        lifecycleService.cancelDeactivationRequest(userId(auth));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('account:request')")
    @GetMapping("/usuarios/me/pedidos")
    public ResponseEntity<List<AccountRequestView>> myRequests(Authentication auth) {
        return ResponseEntity.ok(lifecycleService.myRequests(userId(auth)));
    }

    // ------------------------------------------------------------------ conta desativada (sem login)

    /** Público (fica em /api/auth/**): a conta desativada não consegue entrar para pedir. */
    @PostMapping("/auth/reativacao")
    public ResponseEntity<Map<String, String>> requestReactivation(@RequestBody ReactivationRequest body) {
        lifecycleService.requestReactivation(body.username(), body.password(), body.reason());
        return ResponseEntity.accepted().body(Map.of("message",
                "Se os dados estiverem corretos e a conta puder ser reativada, o pedido foi enviado ao administrador."));
    }

    // ------------------------------------------------------------------ root e admin_users
    // root (account_requests:manage): todos os pedidos.
    // admin_users (account_requests:reactivate): só reativação de conta desativada por inatividade
    // (a regra fina fica no AccountLifecycleService).

    /** ?status=PENDING para a fila de análise; sem status, os 200 mais recentes. */
    @PreAuthorize("hasAnyAuthority('account_requests:manage', 'account_requests:reactivate')")
    @GetMapping("/usuarios/pedidos")
    public ResponseEntity<List<AccountRequestView>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(lifecycleService.listRequests(status));
    }

    @PreAuthorize("hasAnyAuthority('account_requests:manage', 'account_requests:reactivate')")
    @PostMapping("/usuarios/pedidos/{id}/aprovar")
    public ResponseEntity<AccountRequestView> approve(@PathVariable Long id,
                                                      @RequestBody(required = false) DecisionRequest body,
                                                      Authentication auth) {
        return ResponseEntity.ok(lifecycleService.approve(id, userId(auth), body == null ? null : body.note()));
    }

    @PreAuthorize("hasAnyAuthority('account_requests:manage', 'account_requests:reactivate')")
    @PostMapping("/usuarios/pedidos/{id}/recusar")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @RequestBody(required = false) DecisionRequest body,
                                       Authentication auth) {
        lifecycleService.reject(id, userId(auth), body == null ? null : body.note());
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUserId();
    }
}
