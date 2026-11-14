package searchoteca.components;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o @Scheduled (fila de espera; depois também a desativação por inatividade da RF17). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}