package searchoteca.v0.service;

import org.springframework.stereotype.Service;
<<<<<<< Updated upstream:src/main/java/searchoteca/v0/service/DepartmentService.java
import searchoteca.v0.model.BookModel;
import searchoteca.v0.model.DepartmentModel;
import searchoteca.v0.model.LocationModel;
import searchoteca.v0.repository.BookRepository;
import searchoteca.v0.repository.DepartmentRepository;
import searchoteca.v0.repository.LocationRepository;
=======
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.CopyModel;
import searchoteca.model.DepartmentModel;
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;
import searchoteca.repository.DepartmentRepository;
import searchoteca.repository.LocationRepository;
>>>>>>> Stashed changes:src/main/java/searchoteca/service/DepartmentService.java

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final LocationRepository locationRepository;
    
    public DepartmentService(DepartmentRepository departmentRepository, BookRepository bookRepository, LocationRepository locationRepository, CopyRepository copyRepository) {
        this.departmentRepository = departmentRepository;
        this.bookRepository = bookRepository;
        this.locationRepository = locationRepository;
        this.copyRepository = copyRepository;
    }

    public List<DepartmentModel> findAll(){
        return (List<DepartmentModel>) departmentRepository.findAll();
    }

    public DepartmentModel findByDepartCode(String departCode){
        return departmentRepository.findByDepartCode(departCode);
    }
    public DepartmentModel create(DepartmentModel department){
        return departmentRepository.save(department);
    }

    public DepartmentModel update(String departCode, DepartmentModel departInfo){
        DepartmentModel department = departmentRepository.findByDepartCode(departCode);
        department.setDepartName(departInfo.getDepartName());
        department.setDepartDesc(departInfo.getDepartDesc());
        return departmentRepository.save(department);
    }

    public void delete(String departCode){
        departmentRepository.delete(departmentRepository.findByDepartCode(departCode));
    }

    public List<LocationModel> findLocalByDepartCode(String localCode){
        return locationRepository.findByDepartCode(localCode);
    }
    public List<BookModel> findBookByDepartCode(String departCode){
<<<<<<< Updated upstream:src/main/java/searchoteca/v0/service/DepartmentService.java
        return bookRepository.findByDepartCode(departCode);
=======
        if (departCode == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }
         //captura o isbn de todas as cópias no departamento e devolve o registro de livros
        List<CopyModel> copies = copyRepository.findByDepartCode(departCode);
        List<BookModel> books = new ArrayList<>();
        for (CopyModel copy : copies) {
            books.add(bookRepository.findByIsbn(copy.getIsbn()));
        }
        return books;
>>>>>>> Stashed changes:src/main/java/searchoteca/service/DepartmentService.java
    }


}
