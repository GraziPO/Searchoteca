package searchoteca.v0.service;

import org.springframework.stereotype.Service;
import searchoteca.v0.model.BookModel;
import searchoteca.v0.model.LocationModel;
import searchoteca.v0.repository.BookRepository;
import searchoteca.v0.repository.LocationRepository;

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
        return (List<LocationModel>) locationRepository.findAll();
    }

    public LocationModel findByLocalCode(String localCode){
        return locationRepository.findByLocalCode(localCode);
    }

    public LocationModel create(LocationModel location){
        return locationRepository.save(location);
    }

    public LocationModel update(String localCode, LocationModel localInfo){
        LocationModel location = locationRepository.findByLocalCode(localCode);
        location.setLocalName(localInfo.getLocalCode());
        location.setLocalDesc(localInfo.getLocalDesc());
        location.setDepartCode(localInfo.getDepartCode());
        return locationRepository.save(location);
    }

    public void delete(String localCode){
        locationRepository.deleteByLocalCode(localCode);
    }

    public List<BookModel> findBooksByLocalCode(String localCode){
        return bookRepository.findByLocalCode(localCode);
    }
}
