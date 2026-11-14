package searchoteca.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Atalhos para consultar o usuário logado dentro dos services. */
public final class CurrentUser {

    private CurrentUser() {}

    /** O usuário logado tem esta permissão? (o root tem todas) */
    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
