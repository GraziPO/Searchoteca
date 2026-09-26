package searchoteca.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import searchoteca.model.UserModel;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails{
    private final UserModel user;
    private final List<GrantedAuthority> permissions;

    public CustomUserDetails(UserModel user, List<String> permissionCode, boolean isRoot) {
        this.user = user;
        this.permissions = permissionCode.stream()
                .map(SimpleGrantedAuthority:: new)
                .collect(java.util.stream.Collectors.toList());
        
        this.permissions.add(new SimpleGrantedAuthority("ROLE_" + user.getRole_code().toUpperCase()));
        if(isRoot) {
            this.permissions.add(new SimpleGrantedAuthority("ROLE_ROOT"));
        }
    }

    public Long getUserId(){
        return user.getId();
    }
    public String getRoleCode(){
        return user.getRole_code();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return permissions;
    }

    @Override
    public String getPassword(){
        return user.getPassword();
    }

    @Override
    public String getUsername(){
        return user.getUsername();
    }

    @Override
    public boolean isEnabled(){
        return user.getStatus();
    }

    @Override
    public boolean isAccountNonLocked(){
        return user.getStatus();
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }
}