package searchoteca.v0.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/location")
public class LocationController {

    @GetMapping("/")
    public String GetLocation(@RequestParam String Department_id){
        return "Retorna todas as prateleiras de um detereminado departamento";
    };

    @GetMapping("/{id}")
    public String GetLocationByID(@PathVariable String isbn){
        return "informações da prateleira específica (nome, descrição e capacidade)";
    };

    @PostMapping("/")
    public String PostLocation(@RequestBody int isbn){
        return "localização cadastrada";
    };

    @DeleteMapping("/{id}")
    public String DeleteLocation(@PathVariable String isbn){
        return "localização deletada";
    };
}
}
