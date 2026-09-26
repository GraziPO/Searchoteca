package searchoteca.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import searchoteca.model.PermissionModel;
import searchoteca.model.RoleModel;
import searchoteca.model.UserModel;
import searchoteca.repository.AuthorizationRepository;
import searchoteca.repository.PermissionRepository;
import searchoteca.repository.RoleRepository;
import searchoteca.repository.UserRepository;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuthorizationRepository authorizationRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PermissionRepository permissionRepository,
                                    AuthorizationRepository authorizationRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        RoleModel role = roleRepository.findByRoleCode(user.getRole_code());
        boolean isRoot = role !=null && role.isRoot();

        List<String> permissionCodes = isRoot
                ? StreamSupport.stream(permissionRepository.findAll().spliterator(), false)
                .map(PermissionModel::getPermissionCode)
                .toList()
                : authorizationRepository.findPermissionByUsername(user.getUsername());

        return new CustomUserDetails(user,permissionCodes, isRoot);
    }
}
