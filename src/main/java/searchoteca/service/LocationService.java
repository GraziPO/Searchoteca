package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.CopyModel;
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;
import searchoteca.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;

    public LocationService(LocationRepository locationRepository, BookRepository bookRepository, CopyRepository copyRepository) {
        this.locationRepository= locationRepository;
        this.bookRepository= bookRepository;
        this.copyRepository= copyRepository;
    }

    public List<LocationModel> findAll(){
        List<LocationModel> location;
        location =  (List<LocationModel>) locationRepository.findAll();

        return location;   // lista vazia é resposta válida, não erro
    }

    public LocationModel findByLocalCode(String localCode){
        if(localCode == null){
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        return locationRepository.findByLocalCode(localCode);
    }

    public LocationModel create(LocationModel location){
        if (location.getLocalCode() != null && locationRepository.existsByLocalCode(location.getLocalCode())) {
            throw new ResourceConflictException("Já existe uma localização com esse código.");
        }
        return locationRepository.save(location);
    }

    public LocationModel update(String localCode, LocationModel localInfo){
        LocationModel location = locationRepository.findByLocalCode(localCode);

        if(location == null){
            throw new ResourceNotFoundException("Registro inexistente");
        }
        location.setLocalName(localInfo.getLocalName());
        location.setLocalDesc(localInfo.getLocalDesc());
        location.setDepartCode(localInfo.getDepartCode());

        return locationRepository.save(location);
    }

    public void delete(String localCode){
        LocationModel local =  locationRepository.findByLocalCode(localCode);

        if(local == null){
            throw new ResourceNotFoundException("registro não encontrado");
        }
        if (!copyRepository.findByLocalCode(localCode).isEmpty()) {
            throw new ResourceConflictException("Não é possível excluir: há exemplares guardados nesta localização.");
        }
        locationRepository.deleteByLocalCode(localCode);
    }

    public List<BookModel> findBookByLocalCode(String localCode){
        if (localCode == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }

        //fetches all the copies linked to the localCode, searches the corresponding books and return them
        List<String> isbnList = copyRepository.findByLocalCode(localCode)
                .stream()
                .map(CopyModel::getIsbn)
                .distinct()
                .toList();

        List<BookModel> books =  new ArrayList<>();
        for (String isbn : isbnList) {
           books.add(bookRepository.findByIsbn(isbn));
        }

        return books;
    }
}
