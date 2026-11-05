package searchoteca.resetPassword;

import org.springframework.stereotype.Component;
import searchoteca.exception.ResourceConflictException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordPolicy {
    public void validatePassword(String password){
        List<String> errors = new ArrayList<>();

        if(password == null || password.length() < 8){
            errors.add("Senha é muito curta");
        }else{
            if(password.getBytes(StandardCharsets.UTF_8).length >72){
                errors.add("Senha pode ter, no máximo, 72 caracteres");
            }

            if(!password.matches(".*[A-Za-z].*")) errors.add("Senha deve conter ao menos uma letra");
            if(!password.matches(".*\\d.*")) errors.add("Senha deve conter ao menos um numero");
        }

        if(!errors.isEmpty()){
            throw new ResourceConflictException("A senha deve " + String.join(", ", errors) + ".");
        }
    }
}
