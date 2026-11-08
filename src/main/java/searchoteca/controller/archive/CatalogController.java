package searchoteca.controller.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.service.catalog.CatalogService;

@RestController
@RequestMapping("/api/public/acervo")
public class CatalogController {
    private final CatalogService catalogService;


    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20")int size){
        return ResponseEntity.ok(catalogService.search(q,page,size));
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<?> bookDetail(@PathVariable String isbn){
        return ResponseEntity.ok(catalogService.detail(isbn));
    }
}
