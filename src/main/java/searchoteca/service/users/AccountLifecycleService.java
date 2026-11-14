package searchoteca.service.users;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.users.account.AccountRequestView;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.StudentModel;
import searchoteca.model.UserModel;
import searchoteca.repository.users.AccountLifecycleRepository;
import searchoteca.repository.users.AccountLifecycleRepository.PersonalData;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.users.UserRepository;
import searchoteca.security.CurrentUser;
import searchoteca.service.WaitlistService;
import searchoteca.service.settings.SettingsService;

import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida da conta do aluno.
 *
 *  - RF17: aluno sem empréstimo nem devolução há N dias (configurado pelo root, padrão 90) é desativado
 *    pelo job da madrugada. É reversível: o root reativa, ou aprova um pedido de reativação.
 *  - RF19: o aluno pede a desativação; só o root aprova. Ao aprovar, a conta é desativada e
 *    os dados pessoais são anonimizados (LGPD). Isso não tem volta.
 *
 * Quem decide os pedidos:
 *  - account_requests:manage (só o root): qualquer pedido.
 *  - account_requests:reactivate (admin_users): só pedidos de REATIVAÇÃO de contas desativadas por inatividade.
 */
@Service
public class AccountLifecycleService {

    public static final String DEACTIVATE = "DEACTIVATE";
    public static final String REACTIVATE = "REACTIVATE";

    public static final String PERM_MANAGE = "account_requests:manage";
    public static final String PERM_REACTIVATE = "account_requests:reactivate";
    public static final String REASON_INACTIVITY = "INACTIVITY";

    private final AccountLifecycleRepository repository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final WaitlistService waitlistService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SettingsService settingsService;

    public AccountLifecycleService(AccountLifecycleRepository repository,
                                   UserRepository userRepository,
                                   StudentRepository studentRepository,
                                   WaitlistService waitlistService,
                                   PasswordEncoder passwordEncoder,
                                   AuditLogService auditLogService,
                                   SettingsService settingsService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.waitlistService = waitlistService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.settingsService = settingsService;
    }

    // ===================================================================== RF17: inatividade

    /** Chamado pelo StudentInactivityScheduler uma vez por dia. */
    @Transactional
    public int deactivateInactiveStudents() {
        int inactivityDays = settingsService.inactivityDays();   // lido a cada execução: a mudança do root vale na próxima madrugada
        List<Long> userIds = repository.findInactiveStudentUserIds(inactivityDays);
        for (Long userId : userIds) {
            repository.markDeactivated(userId, "INACTIVITY");
            auditLogService.save(null, AuditAction.DEACTIVATE_USER,
                    "user_id " + userId + " desativado por " + inactivityDays + " dias sem movimentação");
        }
        return userIds.size();
    }

    // ===================================================================== RF19: pedidos do aluno

    @Transactional
    public AccountRequestView requestDeactivation(Long userId, String reason) {
        StudentModel student = studentOf(userId);
        if (repository.hasPendingRequest(userId)) {
            throw new ResourceConflictException("Você já tem um pedido aguardando a análise do administrador.");
        }
        if (repository.hasOpenLoans(student.getStudentId())) {
            throw new ResourceConflictException("Devolva os livros emprestados antes de pedir a desativação da conta.");
        }
        long id = repository.insertRequest(userId, DEACTIVATE, trim(reason));
        auditLogService.record(AuditAction.DEACTIVATE_REQUEST, "pedido " + id + " (user_id " + userId + ")");
        return repository.findByUser(userId).getFirst();
    }

    /** O aluno desistiu do pedido antes de o root decidir. */
    @Transactional
    public void cancelDeactivationRequest(Long userId) {
        if (repository.cancelPending(userId, DEACTIVATE) == 0) {
            throw new ResourceNotFoundException("Não há pedido de desativação em aberto.");
        }
    }

    @Transactional(readOnly = true)
    public List<AccountRequestView> myRequests(Long userId) {
        return repository.findByUser(userId);
    }

    /**
     * Pedido de reativação de quem foi desativado por inatividade. A conta não consegue fazer login,
     * então o aluno prova quem é com usuário e senha. A resposta é sempre a mesma, para não revelar
     * se o usuário existe ou se a senha estava certa.
     */
    @Transactional
    public void requestReactivation(String username, String password, String reason) {
        UserModel user = username == null ? null : userRepository.findByUsername(username.trim());
        if (user == null || password == null
                || !"students".equals(user.getRole_code())
                || Boolean.TRUE.equals(user.getStatus())
                || repository.isAnonymized(user.getUserId())
                || !passwordEncoder.matches(password, user.getPassword())
                || repository.hasPendingRequest(user.getUserId())) {
            return;
        }
        long id = repository.insertRequest(user.getUserId(), REACTIVATE, trim(reason));
        auditLogService.save(user.getUsername(), AuditAction.ACTIVATE_REQUEST, "pedido " + id);
    }

    // ===================================================================== root decide

    @Transactional(readOnly = true)
    public List<AccountRequestView> listRequests(String status) {
        String st = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        // o root vê tudo; o admin_users só os pedidos de reativação
        String type = CurrentUser.hasAuthority(PERM_MANAGE) ? null : REACTIVATE;
        return repository.findByStatus(st, type);
    }

    @Transactional
    public AccountRequestView approve(Long requestId, Long rootUserId, String note) {
        AccountRequestView request = pendingRequest(requestId);
        requireCanDecide(request);

        if (DEACTIVATE.equals(request.type())) {
            StudentModel student = studentOf(request.userId());
            if (repository.hasOpenLoans(student.getStudentId())) {
                throw new ResourceConflictException("O aluno ainda tem empréstimo em aberto. A conta só pode ser anonimizada depois da devolução.");
            }
            // decide antes de anonimizar: o registro do pedido fica com a data e o root que aprovou
            repository.decide(requestId, "APPROVED", rootUserId, trim(note));
            anonymize(request.userId());
        } else {
            if (repository.markReactivated(request.userId()) == 0) {
                throw new ResourceConflictException("Esta conta foi anonimizada e não pode ser reativada.");
            }
            repository.decide(requestId, "APPROVED", rootUserId, trim(note));
            auditLogService.record(AuditAction.ACTIVATE_USER, "user_id " + request.userId() + " reativado (pedido " + requestId + ")");
        }
        auditLogService.record(AuditAction.ACCOUNT_REQUEST_APPROVED, "pedido " + requestId + " (" + request.type() + ")");
        return repository.findByUser(request.userId()).stream()
                .filter(r -> r.requestId().equals(requestId)).findFirst().orElseThrow();
    }

    @Transactional
    public void reject(Long requestId, Long rootUserId, String note) {
        AccountRequestView request = pendingRequest(requestId);
        requireCanDecide(request);
        repository.decide(requestId, "REJECTED", rootUserId, trim(note));
        auditLogService.record(AuditAction.ACCOUNT_REQUEST_REJECTED, "pedido " + requestId + " (" + request.type() + ")");
    }

    // ===================================================================== anonimização

    private void anonymize(Long userId) {
        PersonalData data = repository.findPersonalData(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado."));

        // sai das filas de espera; reservas passam para o próximo
        waitlistService.cancelAllForStudent(data.studentId());

        // senha aleatória que ninguém conhece: a conta nunca mais entra
        String unusable = passwordEncoder.encode(UUID.randomUUID().toString());
        repository.anonymize(data, unusable);

        // depois da troca, o registro já não guarda quem era (só o id interno)
        auditLogService.record(AuditAction.USER_ANONYMIZED, "user_id " + userId + " anonimizado a pedido do titular");
    }

    // ===================================================================== auxiliares

    private AccountRequestView pendingRequest(Long requestId) {
        AccountRequestView request = repository.findForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
        if (!"PENDING".equals(request.status())) {
            throw new ResourceConflictException("Este pedido já foi decidido (" + request.status() + ").");
        }
        return request;
    }

    /**
     * Root decide qualquer pedido. O admin_users só decide reativação de conta desativada por inatividade;
     * desativação/anonimização e contas desativadas por outro motivo ficam com o root.
     */
    private void requireCanDecide(AccountRequestView request) {
        if (CurrentUser.hasAuthority(PERM_MANAGE)) {
            return;
        }
        if (DEACTIVATE.equals(request.type())) {
            throw new AccessDeniedException("Pedidos de desativação e anonimização só podem ser decididos pelo root.");
        }
        if (!CurrentUser.hasAuthority(PERM_REACTIVATE) || !REASON_INACTIVITY.equals(request.deactivationReason())) {
            throw new AccessDeniedException("O administrador só reativa contas desativadas por inatividade. "
                    + "Esta conta precisa ser reativada pelo root.");
        }
    }

    private StudentModel studentOf(Long userId) {
        StudentModel student = studentRepository.findByUserId(userId);
        if (student == null) {
            throw new ResourceNotFoundException("Esta conta não é de aluno.");
        }
        return student;
    }

    private static String trim(String text) {
        if (text == null || text.isBlank()) return null;
        String t = text.trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }
}
