package searchoteca.service;

import org.springframework.stereotype.Service;
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

import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final LocationRepository locationRepository;
    
    public DepartmentService(DepartmentRepository departmentRepository, BookRepository bookRepository, LocationRepository locationRepository,  CopyRepository copyRepository) {
        this.departmentRepository = departmentRepository;
        this.bookRepository = bookRepository;
        this.locationRepository = locationRepository;
        this.copyRepository = copyRepository;
    }


    public List<DepartmentModel> findAll(){
        List<DepartmentModel> departments;
        departments = (List<DepartmentModel>) departmentRepository.findAll();

        return departments;   // lista vazia é resposta válida, não erro
    }


    public DepartmentModel findByDepartCode(String departCode){
        if(departmentRepository.findByDepartCode(departCode)==null){
            throw new ResourceNotFoundException("Departamento não encontrado / não existente");
        }
        return departmentRepository.findByDepartCode(departCode);
    }
    public DepartmentModel create(DepartmentModel department){
        return departmentRepository.save(department);
    }


    public DepartmentModel update(String departCode, DepartmentModel departInfo){
        DepartmentModel department = departmentRepository.findByDepartCode(departCode);

        if (department ==null){
            throw new ResourceNotFoundException("Departamento inexistente");
        }
        department.setDepartName(departInfo.getDepartName());
        department.setDepartDesc(departInfo.getDepartDesc());
        return departmentRepository.save(department);
    }


    public void delete(String departCode){
        DepartmentModel department = departCode == null ? null : departmentRepository.findByDepartCode(departCode);
        if (department == null){
            throw new ResourceNotFoundException("Departamento não existente");
        }
        // Só bloqueia se houver algo realmente ligado ao departamento
        if (!locationRepository.findByDepartCode(departCode).isEmpty()
                || !copyRepository.findByDepartCode(departCode).isEmpty()){
            throw new ResourceConflictException("Este departamento não pode ser deletado, pois há localizações ou exemplares ligados a ele");
        }
        departmentRepository.delete(department);
    }


    public List<LocationModel> findLocalByDepartCode(String localCode){
        if (localCode == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }
        return locationRepository.findByDepartCode(localCode);
    }


    public List<BookModel> findBookByDepartCode(String departCode){
        if (departCode == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }

        //fetches all the copies linked to the localCode, searches the corresponding books and return them
        List<String> isbnList = copyRepository.findByDepartCode(departCode)
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
