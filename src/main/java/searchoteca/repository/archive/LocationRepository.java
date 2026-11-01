package searchoteca.repository.archive;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.LocationModel;

import java.util.List;

public interface LocationRepository extends CrudRepository<LocationModel,Long> {
    LocationModel findByLocalCode(String localCode);

    void deleteByLocalCode(String localCode);

    boolean existsByLocalCode(String localCode);

    List<LocationModel> findByDepartCode(String departCode);
}
