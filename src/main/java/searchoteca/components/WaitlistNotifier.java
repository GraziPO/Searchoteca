package searchoteca.components;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import searchoteca.service.EmailService;
import searchoteca.model.waitingLine.WaitlistReservedEvent;
import searchoteca.repository.waitlist.WaitlistQueryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WaitlistNotifier {

    private final WaitlistQueryRepository queryRepository;
    private final EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(WaitlistNotifier.class);

    public WaitlistNotifier(WaitlistQueryRepository queryRepository, EmailService emailService) {
        this.queryRepository = queryRepository;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReserved(WaitlistReservedEvent event) {
        queryRepository.findNotice(event.waitlistId()).ifPresentOrElse(
                n -> emailService.sendWaitlistReady(n.email(), n.studentName(), n.title(),
                        n.copyCode(), n.localName(), n.expiresAt()),
                () -> log.warn("Reserva {} não encontrada para enviar o aviso", event.waitlistId()));
    }
}
