package searchoteca.v0.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.v0.model.LocationModel;
import searchoteca.v0.service.LocationService;

@RestController
@RequestMapping("/localizacao")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService){
        this.locationService=locationService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(locationService.findAll());
    }

    @GetMapping("/{localCode}")
    public ResponseEntity<?> getById(@PathVariable String localCode){
        return ResponseEntity.ok(locationService.findByLocalCode(localCode));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LocationModel location){
        return ResponseEntity.ok(locationService.create(location));
    }

    @PutMapping("/{localCode}")
    public ResponseEntity<?> update(@PathVariable String localCode, @RequestBody LocationModel local){
        return ResponseEntity.ok(locationService.update(localCode, local));
    }

    @DeleteMapping("/{localCode}")
    public ResponseEntity<?> delete(@PathVariable String localCode){
        locationService.delete(localCode);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{localCode}/livros")
    public ResponseEntity<?> getByBookCode(@PathVariable String localCode){
        return ResponseEntity.ok(locationService.findBooksByLocalCode(localCode));
    }
}

