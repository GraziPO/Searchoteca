package searchoteca.legal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchoteca.DTO.LegalDocumentResponse;

/** Público: os documentos precisam poder ser lidos antes do login. */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private final TermsService termsService;

    public LegalController(TermsService termsService) {
        this.termsService = termsService;
    }

    @GetMapping("/termos")
    public ResponseEntity<LegalDocumentResponse> terms() {
        return ResponseEntity.ok(termsService.terms());
    }

    @GetMapping("/privacidade")
    public ResponseEntity<LegalDocumentResponse> privacy() {
        return ResponseEntity.ok(termsService.privacy());
    }
}
