package searchoteca.service.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.archive.CopyLabel;
import searchoteca.DTO.archive.CopyRequest;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.CopyModel;
import searchoteca.model.LocationModel;
import searchoteca.model.status.CopyStatus;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.archive.LocationRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopyService {
    private final CopyRepository copyRepository;
    private final LocationRepository locationRepository;
    private final BookRepository bookRepository;

    public CopyService(CopyRepository copyRepository, LocationRepository locationRepository, BookRepository bookRepository) {
        this.copyRepository = copyRepository;
        this.locationRepository = locationRepository;
        this.bookRepository = bookRepository;
    }

    public List<CopyModel> findAllByBook(String isbn) {
        return copyRepository.findAllByIsbn(isbn);
    }


    @Transactional
    public List<CopyLabel> create(CopyRequest request) {
        if(request.quantity()< 1 || request.quantity()>100) {
            throw new ResourceConflictException("A quantidade deve estar entre 1 e 100");
        }
        if(bookRepository.findByIsbn(request.isbn()) == null){
            throw new ResourceNotFoundException("Este livro não existe");
        }
        LocationModel local = locationRepository.findByLocalCode(request.localCode());
        if(local == null){
            throw new ResourceNotFoundException("Localização não encontrada");
        }
        if(!local.getDepartCode().equals(request.departCode())){
            throw new ResourceConflictException("Esta localização não pertence ao departamento");
        }

        int lastCreatedIndex = copyRepository.findMaxIndexByIsbn(request.isbn());

        List<CopyModel> copies = new ArrayList<>();

        for(int i = 1; i < request.quantity(); i++) {
            CopyModel copy = new CopyModel();
            copy.setIsbn(request.isbn());
            copy.setLocalCode(request.localCode());
            copy.setDepartCode(request.departCode());
            copy.setCopy_index(lastCreatedIndex + i);
            copy.setStatus(CopyStatus.AVAILABLE.name());
            copies.add(copy);
        }

        List<CopyModel> saved = new ArrayList<>();
        copyRepository.saveAll(copies).forEach(saved::add);
        return findLabels(saved.stream().map(CopyModel::getCopyId).toList());
    }

    public List<CopyLabel> findLabels(List<Long> copyIds){
        if(copyIds == null || copyIds.isEmpty() ){
            throw new ResourceNotFoundException("informe ao meonso um exemplar");
        }

        List<CopyModel> copies = new ArrayList<>();
        copyRepository.findAllById(copyIds).forEach(copies::add);

        if(copies.size()!=copyIds.size()){
            Set<Long> found = copies.stream()
                    .map(CopyModel::getCopyId)
                    .collect(Collectors.toSet());

            List<Long> missing = copyIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("exemplares não encontrados" + missing);
        }

        Set<String> isbns = copies.stream().map(CopyModel::getIsbn).collect(Collectors.toSet());
        Map<String, BookModel> books = bookRepository.findByIsbnIn(isbns).stream()
                .collect(Collectors.toMap(BookModel::getIsbn, b -> b));

        return copies.stream()
                .sorted(Comparator.comparing(CopyModel::getCopy_index))
                .map(c ->{
                    BookModel book = books.get(c.getIsbn());
                    return  new CopyLabel(c.getCopyId(),c.getCopy_custom_code(),
                            book.getTitle(),book.getAuthor(),c.getDepartCode(),c.getLocalCode());
                })
                .toList();
    }

}
