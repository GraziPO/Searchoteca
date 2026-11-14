package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.settings.SettingDetail;
import searchoteca.DTO.settings.SettingsUpdateRequest;
import searchoteca.DTO.settings.SettingsView;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.settings.SettingsService;

import java.util.List;

@RestController
@RequestMapping("/api/configuracoes")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** Valores em vigor. Qualquer usuário logado (o frontend decide o que mostrar). */
    @GetMapping
    public ResponseEntity<SettingsView> current() {
        return ResponseEntity.ok(settingsService.view());
    }

    /** Tela do root: valor, padrão, limites e quem alterou por último. */
    @PreAuthorize("hasAuthority('settings:manage')")
    @GetMapping("/detalhes")
    public ResponseEntity<List<SettingDetail>> details() {
        return ResponseEntity.ok(settingsService.details());
    }

    /** Altera uma ou mais configurações. Só o root (settings:manage não é dada a nenhum perfil). */
    @PreAuthorize("hasAuthority('settings:manage')")
    @PutMapping
    public ResponseEntity<SettingsView> update(@RequestBody SettingsUpdateRequest request, Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return ResponseEntity.ok(settingsService.update(request, user.getUserId()));
    }
}
