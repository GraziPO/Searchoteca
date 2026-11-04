package searchoteca.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchoteca.model.UserModel;
import searchoteca.repository.UserRepository;
import searchoteca.resetPassword.PasswordPolicy;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    //====== métodos CRUD padrão =======

    public UserModel create(UserModel user){
        passwordPolicy.validatePassword(user.getPassword());
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
        up_user.setRole_code(user.getRole_code());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            passwordPolicy.validatePassword(user.getPassword());
            up_user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
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
