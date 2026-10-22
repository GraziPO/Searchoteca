package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.UserModel;

import java.util.List;

public interface UserRepository extends CrudRepository<UserModel, Long> {
    UserModel findByUsername(String username);

    UserModel findByStudentId(Long studentId);

    UserModel getUserByCompleteName(String username);

    List<UserModel> findByCustomId(String customId);

    void deleteByUsername(String username);
}
