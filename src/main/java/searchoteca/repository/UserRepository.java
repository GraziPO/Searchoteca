package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.UserModel;

import java.util.List;

public interface UserRepository extends CrudRepository<UserModel, String> {
    UserModel findByUsername(String username);

    UserModel getUserByCompleteName(String username);

    List<UserModel> findByCustomId(String customId);

    void deleteByUsername(String username);
}
