package searchoteca.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchoteca.model.UserModel;
import searchoteca.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //====== métodos CRUD padrão =======

    public UserModel create(UserModel user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(true);
        return userRepository.save(user);
    }

    public List<UserModel> findAll(){
        return (List<UserModel>)userRepository.findAll();
    }

    public UserModel findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    public UserModel update(String username, UserModel user){
        UserModel up_user = userRepository.findByUsername(username);
        up_user.setUsername(username);
        up_user.setCompleteName(user.getCompleteName());
        up_user.setEmail(user.getEmail());
        up_user.setPassword(user.getPassword());
        up_user.setRole_code(user.getRole_code());

        return userRepository.save(up_user);
    }

    public void activate (String username){
        UserModel user = userRepository.findByUsername(username);
        user.setStatus(true);
        userRepository.save(user);
    }
    public void deactivate(String username){
        UserModel user = userRepository.findByUsername(username);
        user.setStatus(false);
        userRepository.save(user);
    }

}
