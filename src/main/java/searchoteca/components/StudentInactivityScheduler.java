package searchoteca.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import searchoteca.service.users.AccountLifecycleService;

/** RF17: uma vez por dia, desativa os alunos sem movimentação há 90 dias. */
@Component
public class StudentInactivityScheduler {

    private static final Logger log = LoggerFactory.getLogger(StudentInactivityScheduler.class);

    private final AccountLifecycleService lifecycleService;

    public StudentInactivityScheduler(AccountLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    // padrão: todo dia às 3h da manhã, horário do servidor
    @Scheduled(cron = "${students.inactivity-cron:0 0 3 * * *}")
    public void deactivateInactive() {
        try {
            int count = lifecycleService.deactivateInactiveStudents();
            log.info("Inatividade: {} aluno(s) desativado(s)", count);
        } catch (Exception e) {
            log.error("Falha ao desativar alunos inativos", e);
        }
    }
}
