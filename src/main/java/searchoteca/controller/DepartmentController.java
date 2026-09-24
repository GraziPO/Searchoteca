package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.model.DepartmentModel;
import searchoteca.service.DepartmentService;

@RestController
@RequestMapping("/api/departamento")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentRepository){
        this.departmentService=departmentRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @GetMapping("/{departCode}")
    public ResponseEntity<?> getById(@PathVariable String departCode){
        return ResponseEntity.ok(departmentService.findByDepartCode(departCode));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentModel department){
        return ResponseEntity.ok(departmentService.create(department));
    }

    @PutMapping("/{departCode}")
    public ResponseEntity<?> update(@PathVariable String departCode, @RequestBody DepartmentModel departInfo){
        return ResponseEntity.ok(departmentService.update(departCode, departInfo));
    }

    @DeleteMapping("/{departCode}")
    public ResponseEntity<?> delete(@PathVariable String departCode){
        departmentService.delete(departCode);
        return ResponseEntity.noContent().build();
    }

    /*------------------------------ funcões com chave estrangeiras ------------------------------*/

    @GetMapping("/{departCode}/livros")
    public ResponseEntity<?> getBooksById(@PathVariable String departCode){
        return ResponseEntity.ok(departmentService.findBookByDepartCode(departCode));
    }

    @GetMapping("/{departCode}/localizacoes")
    public ResponseEntity<?> getLocationById(@PathVariable String departCode){
        return ResponseEntity.ok(departmentService.findLocalByDepartCode(departCode));
    }
}

