package searchoteca.service.users;

import org.springframework.data.domain.Sort;
import searchoteca.DTO.archive.publicSearch.PageResponse;
import searchoteca.components.Paging;
import searchoteca.DTO.users.UserResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditDiff;
import searchoteca.auditTrail.AuditLogService;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.RoleModel;
import searchoteca.model.StudentModel;
import searchoteca.model.UserModel;
import searchoteca.repository.auth.RoleRepository;
import searchoteca.repository.users.AccountLifecycleRepository;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.users.UserRepository;
import searchoteca.resetPassword.PasswordPolicy;
import searchoteca.security.CurrentUser;
import searchoteca.service.WaitlistService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final String STUDENT_ROLE = "students";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AccountLifecycleRepository lifecycleRepository;
    private final AuditLogService auditLogService;
    private final StudentRepository studentRepository;
    private final WaitlistService waitlistService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy,
                       AccountLifecycleRepository lifecycleRepository,
                       AuditLogService auditLogService,
                       StudentRepository studentRepository,
                       WaitlistService waitlistService) {
        this.lifecycleRepository = lifecycleRepository;
        this.auditLogService = auditLogService;
        this.studentRepository = studentRepository;
        this.waitlistService = waitlistService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    //====== métodos CRUD padrão =======

    @Transactional
    public UserModel create(UserModel user){
        RoleModel role = requireRole(user.getRole_code());
        if ("students".equals(role.getRoleCode())) {
            throw new ResourceConflictException("Alunos são cadastrados pelo cadastro de aluno (RA e telefone).");
        }
        requireRootCallerIf(role.isRoot());

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ResourceConflictException("o username não pode estar vazio.");
        }
        user.setPassword(passwordEncoder.encode(user.getUsername()));
        user.setStatus(true);
        user.setMust_change_password(true);

        return userRepository.save(user);
    }

    public List<UserModel> findAll(){
        return (List<UserModel>)userRepository.findAll();
    }

    /** Listagem paginada, em ordem de username (RNF06). Devolve UserResponse: o hash da senha nunca sai. */
    public PageResponse<UserResponse> findPage(int page, int size){
        return PageResponse.from(userRepository.findAll(Paging.of(page, size, Sort.by("username")))
                .map(UserResponse::from));
    }

    public UserModel findByUsername(String username){
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        return user;
    }

    @Transactional
    public UserModel update(String username, UserModel user){
        UserModel up_user = findByUsername(username);
        RoleModel newRole = user.getRole_code() == null ? null : requireRole(user.getRole_code());

        // mexer num root, ou transformar alguém em root, só o root pode
        requireRootCallerIf(isRootRole(up_user.getRole_code()) || (newRole != null && newRole.isRoot()));

        boolean passwordChanged = user.getPassword() != null && !user.getPassword().isBlank();

        // antes de alterar: o que está no banco x o que chegou (RF22). A senha só aparece como "alterada".
        AuditDiff diff = new AuditDiff()
                .field("nome", up_user.getCompleteName(), user.getCompleteName())
                .field("e-mail", up_user.getEmail(), user.getEmail())
                .field("perfil", up_user.getRole_code(), newRole == null ? up_user.getRole_code() : newRole.getRoleCode())
                .secret("senha", passwordChanged);

        up_user.setUsername(username);
        up_user.setCompleteName(user.getCompleteName());
        up_user.setEmail(user.getEmail());
        if (newRole != null) {
            up_user.setRole_code(newRole.getRoleCode());
        }
        // senha definida pelo admin: só se veio preenchida, e grava em up_user (o usuário do banco)
        if (passwordChanged) {
            passwordPolicy.validatePassword(user.getPassword());
            up_user.setPassword(passwordEncoder.encode(user.getPassword()));
            up_user.setPasswordChangedAt(LocalDateTime.now());   // derruba as sessões abertas (RF18)
            up_user.setMust_change_password(true);               // a pessoa troca no próximo acesso
        }
        UserModel saved = userRepository.save(up_user);

        auditLogService.record(AuditAction.UPDATE, "usuário " + username + ": " + diff);
        return saved;
    }

    // ========================================== ATIVAÇÃO / DESATIVACAO ======================================

    /**
     * Reativa a conta.
     * Aluno: o root reativa qualquer conta; o admin_users só as desativadas por inatividade.
     * Conta anonimizada não volta.
     */
    @Transactional
    public void activate (String username){
        UserModel user = findByUsername(username);

        RoleModel role = roleRepository.findByRoleCode(user.getRole_code());

        requireRootCallerIf(role.isRoot());

        if (STUDENT_ROLE.equals(user.getRole_code())) {
            if (lifecycleRepository.isAnonymized(user.getUserId())) {
                throw new ResourceConflictException("Esta conta foi anonimizada e não pode ser reativada.");
            }
            if (Boolean.TRUE.equals(user.getStatus())) {
                return;   // já está ativa: nada a fazer
            }
            if (!CurrentUser.hasAuthority(AccountLifecycleService.PERM_MANAGE)
                    && !AccountLifecycleService.REASON_INACTIVITY.equals(lifecycleRepository.findDeactivationReason(user.getUserId()))) {
                throw new AccessDeniedException("O administrador só reativa contas desativadas por inatividade. "
                        + "Esta conta precisa ser reativada pelo root.");
            }
            // reativa e zera o relógio da inatividade (RF17)
            lifecycleRepository.markReactivated(user.getUserId());
            return;
        }
        user.setStatus(true);
        userRepository.save(user);
    }

    /**
     * Desativa a conta. Aluno desativado não faz mais nada até ser reativado:
     * não entra no sistema (login e tokens recusados), não pega livro e sai das filas de espera.
     */
    @Transactional
    public void deactivate(String username){
        UserModel user = findByUsername(username);

        RoleModel role = roleRepository.findByRoleCode(user.getRole_code());

        requireRootCallerIf(role.isRoot());

        if (STUDENT_ROLE.equals(user.getRole_code())) {
            // mesmo registro da desativação por inatividade, com o motivo ADMIN
            lifecycleRepository.markDeactivated(user.getUserId(), "ADMIN");

            // sai das filas; se tinha exemplar reservado, a reserva passa para o próximo da fila
            StudentModel student = studentRepository.findByUserId(user.getUserId());
            if (student != null) {
                waitlistService.cancelAllForStudent(student.getStudentId());
            }
            return;
        }
        user.setStatus(false);
        userRepository.save(user);
    }

    // ======================================== LIMITA CRIAÇÃO DE USUÁRIO TIPO ROOT AO ROOT =================================
    /** Busca o perfil pelo código; se não existir, 404 com mensagem clara (em vez de NullPointerException). */
    private RoleModel requireRole(String roleCode) {
        RoleModel role = roleCode == null ? null : roleRepository.findByRoleCode(roleCode);
        if (role == null) {
            throw new ResourceNotFoundException("Perfil de acesso inexistente: " + roleCode);
        }
        return role;
    }

    /** Quando a operação envolve um root, quem pede também precisa ser root (403 caso contrário). */
    private void requireRootCallerIf(boolean involvesRoot) {
        if (!involvesRoot) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean callerIsRoot = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ROOT".equals(a.getAuthority()));
        if (!callerIsRoot) {
            throw new AccessDeniedException("Somente o root pode criar, alterar, ativar ou desativar usuários root.");
        }
    }

    /** Usa o is_root do banco, e não o texto "root": vale para qualquer perfil marcado como root. */
    private boolean isRootRole(String roleCode) {
        RoleModel role = roleCode == null ? null : roleRepository.findByRoleCode(roleCode);
        return role != null && role.isRoot();
    }
}
