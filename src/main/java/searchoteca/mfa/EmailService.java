package searchoteca.mfa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * Envio pela API HTTP do Resend (POST https://api.resend.com/emails),
 * usando o RestClient do próprio Spring. Não precisa de dependência nova no pom.xml.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final String baseUrl;
    private final String from;

    public EmailService(@Value("${resend.api-key:}") String apiKey,
                        @Value("${resend.base-url:https://api.resend.com}") String baseUrl,
                        @Value("${resend.from}") String from) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    /** Roda em outra thread: o login responde sem esperar o Resend. */
    @Async
    public void sendCode(String to, String name, String code, long ttlMinutes) {
        String masked = EmailMasker.mask(to);

        if (apiKey == null || apiKey.isBlank()) {
            log.error("RESEND_API_KEY não configurada: código de MFA NÃO enviado para {}", masked);
            return;
        }

        String text = "Olá, " + name + "!\n\n"
                + "Seu código de verificação é: " + code + "\n"
                + "Ele expira em " + ttlMinutes + " minutos.\n\n"
                + "Se não foi você que tentou entrar, troque sua senha imediatamente.";

        // O nome vem do cadastro do usuário: escapar evita injetar HTML no e-mail
        String html = "<p>Olá, " + HtmlUtils.htmlEscape(name) + "!</p>"
                + "<p>Seu código de verificação é:</p>"
                + "<p style=\"font-size:28px;font-weight:bold;letter-spacing:6px\">" + code + "</p>"
                + "<p>Ele expira em " + ttlMinutes + " minutos.</p>"
                + "<p style=\"color:#666\">Se não foi você que tentou entrar, troque sua senha imediatamente.</p>";

        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", "Seu código de acesso - Searchoteca",
                "text", text,
                "html", html
        );

        try {
            restClient.post()
                    .uri(baseUrl + "/emails")
                    .headers(h -> h.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Código de MFA enviado para {}", masked);
        } catch (RestClientResponseException e) {
            log.error("Resend recusou o envio para {}: HTTP {} - {}",
                    masked, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Falha ao enviar código de MFA para {}", masked, e);
        }
    }
}
