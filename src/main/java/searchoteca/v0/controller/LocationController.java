package searchoteca.v0.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.v0.model.LocationModel;
import searchoteca.v0.repository.LocationRepository;

@RestController
@RequestMapping("/localizacao")
public class LocationController {
    private final LocationRepository locationRepository;

    public LocationController(LocationRepository locationRepository){
        this.locationRepository=locationRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(locationRepository.findAll());
    }

    @GetMapping("/{localCode}")
    public ResponseEntity<?> getById(@PathVariable String localCode){
        return ResponseEntity.ok(locationRepository.findByLocalCode(localCode));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LocationModel location){
        return ResponseEntity.ok(locationRepository.save(location));
    }

    @PutMapping("/{localCode}")
    public ResponseEntity<?> update(@PathVariable String localCode, @RequestBody LocationModel local){
        LocationModel location = locationRepository.findByLocalCode(localCode);
        location.setLocalName(local.getLocalCode());
        location.setLocalDesc(local.getLocalDesc());
        return ResponseEntity.ok(locationRepository.save(location));
    }

    @DeleteMapping("/{localCode}")
    public ResponseEntity<?> delete(@PathVariable String localCode){
        LocationModel location = locationRepository.findByLocalCode(localCode);
        if(!locationRepository.existsByLocalCode(localCode)){
            return ResponseEntity.notFound().build();
        }
        locationRepository.deleteByLocalCode(location.getLocalCode());
        return ResponseEntity.noContent().build();
    }
}

