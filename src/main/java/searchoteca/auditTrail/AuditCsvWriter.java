package searchoteca.auditTrail;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera o CSV da auditoria (RF21) no formato que o Excel em português abre direto:
 * separador ";" , UTF-8 com BOM (acentos certos) e quebra de linha CRLF.
 */
public final class AuditCsvWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AuditCsvWriter() {}

    public static byte[] write(List<AuditLogModel> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("id;data_hora;usuario;acao;detalhes\r\n");
        for (AuditLogModel log : logs) {
            csv.append(log.getId()).append(';')
               .append(cell(log.getTime_stamp() == null ? null : DATE.format(log.getTime_stamp()))).append(';')
               .append(cell(log.getUsername())).append(';')
               .append(cell(log.getAction())).append(';')
               .append(cell(log.getDetails())).append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    /**
     * Uma célula entre aspas, com as aspas internas dobradas.
     * Texto que começa com = + - @ ganha um apóstrofo na frente: sem isso, o Excel trataria
     * como fórmula (e um username digitado no login, como "=HYPERLINK(...)", viraria um link
     * malicioso dentro da planilha do administrador).
     */
    private static String cell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String v = value.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@\t".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
