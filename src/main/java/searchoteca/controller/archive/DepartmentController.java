package searchoteca.controller.archive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.DepartmentModel;
import searchoteca.service.wms.DepartmentService;

@RestController
@RequestMapping("/api/departamento")
public class DepartmentController {
    private final DepartmentService departmentService;
    private final AuditLogService auditLogService;

    public DepartmentController(DepartmentService departmentRepository, AuditLogService auditLogService) {
        this.departmentService=departmentRepository;
        this.auditLogService=auditLogService;
    }

    @PreAuthorize("hasAuthority('org_depart:view')")
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        auditLogService.record(AuditAction.ACCESS, "listagem de departamentos (página " + page + ")");
        return ResponseEntity.ok(departmentService.findPage(page, size));
    }

    @PreAuthorize("hasAuthority('org_depart:view')")
    @GetMapping("/{departCode}")
    public ResponseEntity<?> getById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"department" + departCode);
        return ResponseEntity.ok(departmentService.findByDepartCode(departCode));
    }

    @PreAuthorize("hasAuthority('org_depart:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentModel department){
        DepartmentModel created = departmentService.create(department);
        auditLogService.record(AuditAction.CREATE, "departamento " + created.getDepartCode());
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasAuthority('org_depart:edit')")
    @PutMapping("/{departCode}")
    public ResponseEntity<?> update(@PathVariable String departCode, @RequestBody DepartmentModel departInfo){
        // o registro (com o antes -> depois) é feito no DepartmentService
        return ResponseEntity.ok(departmentService.update(departCode, departInfo));
    }

    @PreAuthorize("hasAuthority('org_depart:delete')")
    @DeleteMapping("/{departCode}")
    public ResponseEntity<?> delete(@PathVariable String departCode){
        departmentService.delete(departCode);
        auditLogService.record(AuditAction.DELETE, "departamento " + departCode);
        return ResponseEntity.noContent().build();
    }

    /*------------------------------ funcões com chave estrangeiras ------------------------------*/

    @PreAuthorize("hasAuthority('org_depart:view')")
    @GetMapping("/{departCode}/livros")
    public ResponseEntity<?> getBooksById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"Books in department: " + departCode);
        return ResponseEntity.ok(departmentService.findBookByDepartCode(departCode));
    }

    @PreAuthorize("hasAuthority('org_depart:view')")
    @GetMapping("/{departCode}/localizacoes")
    public ResponseEntity<?> getLocationById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"Locations in department: " + departCode);
        return ResponseEntity.ok(departmentService.findLocalByDepartCode(departCode));
    }
}

