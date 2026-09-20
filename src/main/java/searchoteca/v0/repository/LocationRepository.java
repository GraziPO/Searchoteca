package searchoteca.v0.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.v0.model.LocationModel;

import java.util.List;

public interface LocationRepository extends CrudRepository<LocationModel,String> {
    LocationModel findByLocalCode(String localCode);

    void deleteByLocalCode(String localCode);

    List<LocationModel> findByDepartCode(String departCode);
}
