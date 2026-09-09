package searchoteca.v0.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livro")
public class BookController {

    @GetMapping("/")
    public String GetBook(){
        return "Retorna todos os livros";
    };

    @GetMapping("/{id}")
    public String GetBookByID(@PathVariable String isbn){
        return "informações do livro";
    };

    @PostMapping("/")
    public String PostBook(@RequestBody int isbn){
        return "livro cadastrado";
    };

    @DeleteMapping("/{id}")
    public String DeleteBook(@PathVariable String isbn){
        return "livro deletado";
    };
}
