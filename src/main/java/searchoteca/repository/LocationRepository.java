package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.LocationModel;

import java.util.List;

public interface LocationRepository extends CrudRepository<LocationModel,Long> {
    LocationModel findByLocalCode(Long localCode);

    void deleteByLocalCode(Long localCode);

    boolean existsByLocalCode(Long localCode);

    List<LocationModel> findByDepartCode(Long departCode);
}
