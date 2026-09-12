package searchoteca.v0.controller;

import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/department")
public class DepartmentController {
    @GetMapping("/")
    public String GetDepartment(){
        return "Retorna todos os departamentos";
    };

    @GetMapping("/{id}")
    public String GetDepartmentByID(@PathVariable String isbn){
        return "informações do departmento";
    };

    @PostMapping("/")
    public String PostDepartment(@RequestBody int isbn){
        return "departamento cadastrado";
    };

    @DeleteMapping("/{id}")
    public String DeleteDepartment(@PathVariable String isbn){
        return "departamento deletado";
    };
}

