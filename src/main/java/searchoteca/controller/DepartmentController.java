package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.DepartmentModel;
import searchoteca.service.DepartmentService;

@RestController
@RequestMapping("/api/departamento")
public class DepartmentController {
    private final DepartmentService departmentService;
    private final AuditLogService auditLogService;

    public DepartmentController(DepartmentService departmentRepository, AuditLogService auditLogService) {
        this.departmentService=departmentRepository;
        this.auditLogService=auditLogService;
    }

    @PreAuthorize("hasAuthority('org_departs:view')")
    @GetMapping
    public ResponseEntity<?> getAll() {
        auditLogService.record(AuditAction.ACCESS,"All Departments");
        return ResponseEntity.ok(departmentService.findAll());
    }

    @PreAuthorize("hasAuthority('org_departs:view')")
    @GetMapping("/{departCode}")
    public ResponseEntity<?> getById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"department" + departCode);
        return ResponseEntity.ok(departmentService.findByDepartCode(departCode));
    }

    @PreAuthorize("hasAuthority('org_departs:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentModel department){
        auditLogService.record(AuditAction.CREATE,"department" + department.getDepartCode());
        return ResponseEntity.ok(departmentService.create(department));
    }

    @PreAuthorize("hasAuthority('org_departs:update')")
    @PutMapping("/{departCode}")
    public ResponseEntity<?> update(@PathVariable String departCode, @RequestBody DepartmentModel departInfo){
        auditLogService.record(AuditAction.UPDATE,"department" + departCode);
        return ResponseEntity.ok(departmentService.update(departCode, departInfo));
    }

    @PreAuthorize("hasAuthority('org_departs:delete')")
    @DeleteMapping("/{departCode}")
    public ResponseEntity<?> delete(@PathVariable String departCode){
        departmentService.delete(departCode);
        auditLogService.record(AuditAction.DELETE,"department" + departCode);
        return ResponseEntity.noContent().build();
    }

    /*------------------------------ funcões com chave estrangeiras ------------------------------*/

    @PreAuthorize("hasAuthority('org_departs:view')")
    @GetMapping("/{departCode}/livros")
    public ResponseEntity<?> getBooksById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"Books in department: " + departCode);
        return ResponseEntity.ok(departmentService.findBookByDepartCode(departCode));
    }

    @PreAuthorize("hasAuthority('org_departs:view')")
    @GetMapping("/{departCode}/localizacoes")
    public ResponseEntity<?> getLocationById(@PathVariable String departCode){
        auditLogService.record(AuditAction.ACCESS,"Locations in department: " + departCode);
        return ResponseEntity.ok(departmentService.findLocalByDepartCode(departCode));
    }
}

