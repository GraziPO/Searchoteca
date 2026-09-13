package searchoteca.v0.service;

import org.springframework.stereotype.Service;
import searchoteca.v0.model.BookModel;
import searchoteca.v0.model.DepartmentModel;
import searchoteca.v0.model.LocationModel;
import searchoteca.v0.repository.BookRepository;
import searchoteca.v0.repository.DepartmentRepository;
import searchoteca.v0.repository.LocationRepository;

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
        return bookRepository.findByDepartCode(departCode);
    }


}
