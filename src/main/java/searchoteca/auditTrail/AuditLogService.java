package searchoteca.auditTrail;

import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import searchoteca.security.CustomUserDetails;

import java.time.LocalDateTime;
import org.slf4j.Logger;

@Service
public class AuditLogService {
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger log  = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository LogRepository;

    public AuditLogService(AuditLogRepository LogRepository) {
        this.LogRepository = LogRepository;
    }

    public void record(AuditAction action, String details){
        String username = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            username = user.getUsername();
        }
        save(username, action, details);
    }

    public void recordLogin(String username){
        save(username, AuditAction.LOGIN_SUCCESS, null);
    }

    public void recordAccessDenied(String username){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        username = auth != null ? auth.getName() : null;
        save(username,AuditAction.ACCESS_DENIED, null);
    }

    public void save(String username, AuditAction action, String details){
        AuditLogModel audit = new AuditLogModel();
        audit.setTime_stamp(LocalDateTime.now());
        // username pode vir do corpo do login (valor digitado): limita ao tamanho da coluna
        audit.setUsername(username != null && username.length() > 30 ? username.substring(0, 30) : username);
        audit.setAction(action.name());
        audit.setDetails(details);

        auditLog.info("action={} user={} details={}", audit.getAction(), audit.getUsername(), audit.getDetails());

        try{
            LogRepository.save(audit);
        }catch(Exception e){
            log.error("Falha ao gravar auditoria: {}", audit.getAction(), e);
        }
    }


}
