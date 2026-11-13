package searchoteca.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.CopyModel;
import searchoteca.model.Books.status.CopyStatus;
import searchoteca.model.StudentModel;
import searchoteca.model.waitingLine.*;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.waitlist.WaitlistRepository;
import searchoteca.repository.waitlist.WaitlistQueryRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WaitlistService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final WaitlistRepository waitlistRepository;
    private final WaitlistQueryRepository writeRepository;
    private final CopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;
    private final int maxSize;
    private final Duration holdTime;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           WaitlistQueryRepository writeRepository,
                           CopyRepository copyRepository,
                           BookRepository bookRepository,
                           StudentRepository studentRepository,
                           AuditLogService auditLogService,
                           ApplicationEventPublisher events,
                           @Value("${waitlist.max-size:3}") int maxSize,
                           @Value("${waitlist.hold-hours:48}") long holdHours) {
        this.waitlistRepository = waitlistRepository;
        this.writeRepository = writeRepository;
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
        this.events = events;
        this.maxSize = maxSize;
        this.holdTime = Duration.ofHours(holdHours);
    }

    /** Situação da fila do título para o aluno logado. */
    @Transactional(readOnly = true)
    public WaitlistSummary summary(Long userId, String isbn) {
        StudentModel student = studentOf(userId);
        requireBook(isbn);
        return buildSummary(student.getStudentId(), isbn);
    }

    /** Filas em que o aluno logado está. */
    @Transactional(readOnly = true)
    public List<WaitlistEntry> mine(Long userId) {
        return writeRepository.findByStudent(studentOf(userId).getStudentId());
    }

    // ======================================================================== operador

    /** Fila completa do título, com nome e RA. */
    @Transactional(readOnly = true)
    public List<WaitlistEntry> queue(String isbn) {
        requireBook(isbn);
        return writeRepository.findQueue(isbn);
    }

    private void requireBook(String isbn) {
        if (isbn == null || bookRepository.findByIsbn(isbn) == null) {
            throw new ResourceNotFoundException("Livro não encontrado.");
        }
    }

    /** Há alguém aguardando este título? Evita trabalho extra ao cadastrar cópias novas. */
    public boolean hasWaiting(String isbn) {
        return waitlistRepository.countWaiting(isbn) > 0;
    }
    @Transactional
    public WaitlistSummary join(Long userId, String isbn) {
        StudentModel student = studentOf(userId);
        requireBook(isbn);

        // a partir daqui, ninguém mais mexe na fila deste título até o commit
        waitlistRepository.lockBook(isbn);

        int[] counts = writeRepository.copyCounts(isbn);
        int available = counts[0], circulating = counts[1];
        if (circulating == 0) {
            throw new ResourceConflictException("Este título não tem exemplares para empréstimo.");
        }
        if (available > 0) {
            throw new ResourceConflictException("Há exemplar disponível deste título: o empréstimo pode ser feito no balcão.");
        }
        if (writeRepository.hasOpenLoanOfTitle(student.getStudentId(), isbn)) {
            throw new ResourceConflictException("Você já está com um exemplar deste título.");
        }
        if (waitlistRepository.findActive(isbn, student.getStudentId()).isPresent()) {
            throw new ResourceConflictException("Você já está na fila de espera deste título.");
        }
        if (waitlistRepository.countActive(isbn) >= maxSize) {
            throw new ResourceConflictException("A fila de espera deste título está cheia (máximo de " + maxSize + " alunos).");
        }

        WaitlistModel entry = new WaitlistModel();
        entry.setIsbn(isbn);
        entry.setStudentId(student.getStudentId());
        entry.setStatus(WaitlistStatus.WAITING.name());
        entry.setCreatedAt(LocalDateTime.now());
        waitlistRepository.save(entry);

        auditLogService.record(AuditAction.WAITLIST_JOINED, "fila do ISBN " + isbn + " — RA " + student.getRa());
        return buildSummary(student.getStudentId(), isbn);
    }

    @Transactional
    public void leave(Long userId, String isbn) {
        StudentModel student = studentOf(userId);
        waitlistRepository.lockBook(isbn);

        WaitlistModel entry = waitlistRepository.findActive(isbn, student.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Você não está na fila de espera deste título."));

        Long reservedCopy = WaitlistStatus.NOTIFIED.name().equals(entry.getStatus()) ? entry.getCopyId() : null;
        close(entry, WaitlistStatus.CANCELLED);
        auditLogService.record(AuditAction.WAITLIST_LEFT, "fila do ISBN " + isbn + " — RA " + student.getRa());

        if (reservedCopy != null) {
            copyRepository.findById(reservedCopy).ifPresent(this::releaseCopy);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public CopyStatus releaseCopy(CopyModel copy) {
        waitlistRepository.lockBook(copy.getIsbn());

        WaitlistModel next = waitlistRepository.findNextWaiting(copy.getIsbn()).orElse(null);
        if (next == null) {
            copy.setStatus(CopyStatus.AVAILABLE.name());
            copyRepository.save(copy);
            return CopyStatus.AVAILABLE;
        }

        LocalDateTime now = LocalDateTime.now();
        next.setStatus(WaitlistStatus.NOTIFIED.name());
        next.setCopyId(copy.getCopyId());
        next.setNotifiedAt(now);
        next.setExpiresAt(now.plus(holdTime));
        waitlistRepository.save(next);

        copy.setStatus(CopyStatus.RESERVED.name());
        copyRepository.save(copy);

        auditLogService.record(AuditAction.WAITLIST_RESERVED,
                "exemplar " + copy.getCopy_custom_code() + " reservado até " + next.getExpiresAt().format(FMT)
                        + " (fila " + next.getWaitlistId() + ")");

        // o e-mail só sai depois do commit (WaitlistNotifier), para não avisar de uma reserva desfeita
        events.publishEvent(new WaitlistReservedEvent(next.getWaitlistId()));
        return CopyStatus.RESERVED;
    }

    /** Empréstimo de um exemplar RESERVED: só o aluno dono da reserva pode levar. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void claimReservation(CopyModel copy, Long studentId) {
        WaitlistModel entry = waitlistRepository.findReservationByCopy(copy.getCopyId()).orElse(null);
        if (entry == null) {
            return; // reserva sem dono (dado inconsistente): trata como disponível
        }
        if (!entry.getStudentId().equals(studentId)) {
            throw new ResourceConflictException("O exemplar " + copy.getCopy_custom_code()
                    + " está reservado para outro aluno da fila de espera até " + entry.getExpiresAt().format(FMT) + ".");
        }
        close(entry, WaitlistStatus.FULFILLED);
        auditLogService.record(AuditAction.WAITLIST_FULFILLED, "fila " + entry.getWaitlistId()
                + " — exemplar " + copy.getCopy_custom_code() + " emprestado");
    }

    /** Depois de um empréstimo, tira o aluno das filas dos títulos que ele levou. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeEntriesAfterLoan(Long studentId, Collection<CopyModel> loaned) {
        Set<Long> loanedIds = loaned.stream().map(CopyModel::getCopyId).collect(Collectors.toSet());
        Set<String> isbns = loaned.stream().map(CopyModel::getIsbn).collect(Collectors.toSet());

        for (String isbn : isbns) {
            waitlistRepository.findActive(isbn, studentId).ifPresent(entry -> {
                Long reserved = entry.getCopyId();
                boolean otherCopyReserved = WaitlistStatus.NOTIFIED.name().equals(entry.getStatus())
                        && reserved != null && !loanedIds.contains(reserved);
                close(entry, WaitlistStatus.FULFILLED);
                if (otherCopyReserved) {
                    copyRepository.findById(reserved).ifPresent(this::releaseCopy);
                }
            });
        }
    }

    /** Reservas vencidas passam para o próximo da fila. Chamado pelo WaitlistScheduler. */
    @Transactional
    public int expireReservations() {
        List<WaitlistModel> expired = waitlistRepository.findExpired(LocalDateTime.now());
        for (WaitlistModel entry : expired) {
            Long copyId = entry.getCopyId();
            close(entry, WaitlistStatus.EXPIRED);
            auditLogService.save(null, AuditAction.WAITLIST_EXPIRED,
                    "fila " + entry.getWaitlistId() + " — reserva não retirada no prazo");

            if (copyId != null) {
                copyRepository.findById(copyId)
                        .filter(c -> CopyStatus.RESERVED.name().equals(c.getStatus()))
                        .ifPresent(this::releaseCopy);
            }
        }
        return expired.size();
    }

    private WaitlistSummary buildSummary(Long studentId, String isbn) {
        int[] counts = writeRepository.copyCounts(isbn);
        int queueSize = (int) waitlistRepository.countActive(isbn);

        WaitlistEntry mine = writeRepository.findByStudent(studentId).stream()
                .filter(e -> e.isbn().equals(isbn))
                .findFirst().orElse(null);

        boolean canJoin = mine == null
                && counts[1] > 0          // existe exemplar em circulação
                && counts[0] == 0         // nenhum disponível
                && queueSize < maxSize
                && !writeRepository.hasOpenLoanOfTitle(studentId, isbn);

        return new WaitlistSummary(isbn, queueSize, maxSize, counts[0], counts[1],
                mine != null,
                mine == null ? null : mine.position(),
                mine == null ? null : mine.status(),
                mine == null ? null : mine.expiresAt(),
                canJoin);
    }

    /**
     * Tira o aluno de todas as filas (conta desativada ou anonimizada).
     * Reservas que ele tinha passam para o próximo da fila.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int cancelAllForStudent(Long studentId) {
        List<WaitlistModel> entries = waitlistRepository.findActiveByStudent(studentId);
        for (WaitlistModel entry : entries) {
            Long reservedCopy = WaitlistStatus.NOTIFIED.name().equals(entry.getStatus()) ? entry.getCopyId() : null;
            close(entry, WaitlistStatus.CANCELLED);
            if (reservedCopy != null) {
                copyRepository.findById(reservedCopy).ifPresent(this::releaseCopy);
            }
        }
        return entries.size();
    }

    private void close(WaitlistModel entry, WaitlistStatus status) {
        entry.setStatus(status.name());
        entry.setClosedAt(LocalDateTime.now());
        waitlistRepository.save(entry);
    }

    private StudentModel studentOf(Long userId) {
        StudentModel student = studentRepository.findByUserId(userId);
        if (student == null) {
            throw new ResourceNotFoundException("Perfil de aluno não encontrado para este usuário.");
        }
        return student;
    }
}
