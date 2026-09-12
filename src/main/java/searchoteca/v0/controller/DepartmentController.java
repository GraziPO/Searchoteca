package searchoteca.v0.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.v0.model.DepartmentModel;
import searchoteca.v0.repository.DepartmentRepository;

@RestController
@RequestMapping("/departamento")
public class DepartmentController {
    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository){
        this.departmentRepository=departmentRepository;
    };

    @GetMapping
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(departmentRepository.findAll());
    };

    @GetMapping("/{departCode}")
    public ResponseEntity<?> getById(@PathVariable String departCode){
        DepartmentModel department = departmentRepository.findByDepartCode(departCode);
        return ResponseEntity.ok((department));
    };

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentModel department){
        return ResponseEntity.ok(departmentRepository.save(department));
    };

    @PutMapping("/{departCode}")
    public ResponseEntity<?> update(@PathVariable String departCode, @RequestBody DepartmentModel departInfo){
        DepartmentModel departament = departmentRepository.findByDepartCode(departCode);
        departament.setDepartName(departInfo.getDepartName());
        departament.setDepartDesc(departInfo.getDepartDesc());
        return ResponseEntity.ok(departmentRepository.save(departament));
    };

    @DeleteMapping("/{departCode}")
    public ResponseEntity<?> delete(@PathVariable String departCode){
        DepartmentModel department = departmentRepository.findByDepartCode(departCode);
        if(!departmentRepository.existsByDepartCode(departCode)){
            return ResponseEntity.notFound().build();
        };
        departmentRepository.deleteByDepartCode(department.getDepartCode());
        return ResponseEntity.noContent().build();
    };
}

