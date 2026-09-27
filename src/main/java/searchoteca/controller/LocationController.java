package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.LocationModel;
import searchoteca.service.LocationService;

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
    public ResponseEntity<?> getAll(){
        auditLogService.record(AuditAction.ACCESS,"All Locations");
        return ResponseEntity.ok(locationService.findAll());
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
        auditLogService.record(AuditAction.CREATE,"location" + location.getLocalCode());
        return ResponseEntity.ok(locationService.create(location));
    }

    @PreAuthorize("hasAuthority('org_local:update')")
    @PutMapping("/{localCode}")
    public ResponseEntity<?> update(@PathVariable String localCode, @RequestBody LocationModel local){
        auditLogService.record(AuditAction.UPDATE,"location" + localCode);
        return ResponseEntity.ok(locationService.update(localCode, local));
    }

    @PreAuthorize("hasAuthority('org_local:delete')")
    @DeleteMapping("/{localCode}")
    public ResponseEntity<?> delete(@PathVariable String localCode){
        auditLogService.record(AuditAction.DELETE,"location" + localCode);
        locationService.delete(localCode);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('org_local:view')")
    @GetMapping("/{localCode}/livros")
    public ResponseEntity<?> getByBookCode(@PathVariable String localCode){
        auditLogService.record(AuditAction.ACCESS,"Books in location: " + localCode);
        return ResponseEntity.ok(locationService.findBookByLocalCode(localCode));
    }
}

