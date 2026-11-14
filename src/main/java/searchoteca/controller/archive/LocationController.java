package searchoteca.controller.archive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.LocationModel;
import searchoteca.service.wms.LocationService;

@RestController
@RequestMapping("/api/localizacao")
public class LocationController {
    private final LocationService locationService;
    private final AuditLogService auditLogService;

    public LocationController(LocationService locationService, AuditLogService auditLogService) {
        this.auditLogService= auditLogService;
        this.locationService=locationService;
    }

    @PreAuthorize("hasAuthority('org_local:view')")
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        auditLogService.record(AuditAction.ACCESS, "listagem de localizações (página " + page + ")");
        return ResponseEntity.ok(locationService.findPage(page, size));
    }

    @PreAuthorize("hasAuthority('org_local:view')")
    @GetMapping("/{localCode}")
    public ResponseEntity<?> getById(@PathVariable String localCode){
        auditLogService.record(AuditAction.ACCESS,"location" + localCode);
        return ResponseEntity.ok(locationService.findByLocalCode(localCode));
    }

    @PreAuthorize("hasAuthority('org_local:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody LocationModel location){
        LocationModel created = locationService.create(location);
        auditLogService.record(AuditAction.CREATE, "localização " + created.getLocalCode());
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasAuthority('org_local:edit')")
    @PutMapping("/{localCode}")
    public ResponseEntity<?> update(@PathVariable String localCode, @RequestBody LocationModel local){
        // o registro (com o antes -> depois) é feito no LocationService
        return ResponseEntity.ok(locationService.update(localCode, local));
    }

    @PreAuthorize("hasAuthority('org_local:delete')")
    @DeleteMapping("/{localCode}")
    public ResponseEntity<?> delete(@PathVariable String localCode){
        locationService.delete(localCode);
        auditLogService.record(AuditAction.DELETE, "localização " + localCode);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('org_local:view')")
    @GetMapping("/{localCode}/livros")
    public ResponseEntity<?> getByBookCode(@PathVariable String localCode){
        auditLogService.record(AuditAction.ACCESS,"Books in location: " + localCode);
        return ResponseEntity.ok(locationService.findBookByLocalCode(localCode));
    }
}

