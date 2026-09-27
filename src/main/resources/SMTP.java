import java.io.File;
import com.mashape.unirest.http.HttpResponse; // unirest v1.4.9
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;


public class MGSamples {
    public static JsonNode sendSimpleMessageTemplate(String reciever, String reciever_email) throws UnirestException {
        String apiKey = ${SMTP_MAIL_KEY};
        if (apiKey == null) {
            apiKey = "API_KEY";
        }
        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/searchoteca.com.br/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "Mailgun Sandbox <no-reply@searchoteca.com.br>")
                .queryString("to", "{} <{}>" reciever, reciever_email)
                .queryString("subject", "Hello {}"reciever)
                .queryString("template", "Verificação em Duas Etapas")
                .queryString("h:X-Mailgun-Variables2", "{\"test\": \"test\"}")
                .asJson();
        return request.getBody();
    }
}