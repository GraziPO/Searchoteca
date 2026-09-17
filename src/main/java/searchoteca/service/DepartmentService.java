package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.DepartmentModel;
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.DepartmentRepository;
import searchoteca.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final BookRepository bookRepository;
    private final LocationRepository locationRepository;
    
    public DepartmentService(DepartmentRepository departmentRepository, BookRepository bookRepository, LocationRepository locationRepository) {
        this.departmentRepository = departmentRepository;
        this.bookRepository = bookRepository;
        this.locationRepository = locationRepository;
    }


    public List<DepartmentModel> findAll(){
        List<DepartmentModel> departments;
        departments = (List<DepartmentModel>) departmentRepository.findAll();

        if(departments.isEmpty()){
            throw new ResourceNotFoundException("Não há departamentos cadastrados");
        }
        return departments;
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
        if (departCode == null){
            throw new ResourceNotFoundException("Departamento não existente");
        }
        if (!departCode.isEmpty()){
            throw new ResourceConflictException("Este departamento não pode ser deletado, pois há registros ligados a ele");
        }
        departmentRepository.delete(departmentRepository.findByDepartCode(departCode));
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
        return bookRepository.findByDepartCode(departCode);
    }


}
