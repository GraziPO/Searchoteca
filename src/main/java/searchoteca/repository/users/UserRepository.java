package searchoteca.repository.users;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.UserModel;

public interface UserRepository extends CrudRepository<UserModel, Long> {
    UserModel findByUsername(String username);

    UserModel findByUserId(Long id);

    UserModel findByEmail(String email);

}
