package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final BookRepository bookRepository;

    public LocationService(LocationRepository locationRepository, BookRepository bookRepository) {
        this.locationRepository= locationRepository;
        this.bookRepository= bookRepository;
    }

    public List<LocationModel> findAll(){
        List<LocationModel> location;
        location =  (List<LocationModel>) locationRepository.findAll();

        if(location.isEmpty()){
            throw new ResourceNotFoundException("Nenhuma localização cadastrada");
        }
        return location;
    }

    public LocationModel findByLocalCode(String localCode){
        if(localCode == null){
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        return locationRepository.findByLocalCode(localCode);
    }

    public LocationModel create(LocationModel location){
        if(location.getLocalCode() != null){
            throw new ResourceConflictException("Livro já cadastrado");
        }
        return locationRepository.save(location);
    }

    public LocationModel update(String localCode, LocationModel localInfo){
        LocationModel location = locationRepository.findByLocalCode(localCode);

        if(location == null){
            throw new ResourceNotFoundException("Registro inexistente");
        }
        location.setLocalName(localInfo.getLocalCode());
        location.setLocalDesc(localInfo.getLocalDesc());
        location.setDepartCode(localInfo.getDepartCode());

        return locationRepository.save(location);
    }

    public void delete(String localCode){
        LocationModel local =  locationRepository.findByLocalCode(localCode);

        if(local == null){
            throw new ResourceNotFoundException("registro não encontrado");
        }
        locationRepository.deleteByLocalCode(localCode);
    }

    public List<BookModel> findBooksByLocalCode(String localCode){
        LocationModel location =  locationRepository.findByLocalCode(localCode);
        if(location == null){
            throw new ResourceNotFoundException("Registro inexistente");
        }
        return bookRepository.findByLocalCode(localCode);
    }
}
