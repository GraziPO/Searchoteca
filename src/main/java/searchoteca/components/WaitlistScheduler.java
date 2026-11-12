package searchoteca.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import searchoteca.service.WaitlistService;

@Component
public class WaitlistScheduler {

    private final WaitlistService waitlistService;

    private static final Logger log = LoggerFactory.getLogger(WaitlistScheduler.class);

    public  WaitlistScheduler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    // a cada 10 minutos (configurável), começando 1 minuto depois de a aplicação subir
    @Scheduled(fixedDelayString = "${waitlist.expiry-check-ms:600000}", initialDelay = 60000)
    public void expireReservations() {
        try {
            int expired = waitlistService.expireReservations();
            if (expired > 0) {
                log.info("Fila de espera: {} reserva(s) expirada(s) repassada(s)", expired);
            }
        } catch (Exception e) {
            log.error("Falha ao processar reservas vencidas da fila de espera", e);
        }
    }
}