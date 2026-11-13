package searchoteca.auditTrail;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Monta o "antes -> depois" de uma edição para a trilha de auditoria (RF22).
 *
 *   new AuditDiff()
 *       .field("nome", antigo.getNome(), novo.getNome())
 *       .secret("senha", senhaFoiTrocada)
 *       .toString();
 *   // nome: "Exatas" -> "Ciências Exatas"; senha: alterada
 *
 * Só entram os campos que mudaram. Texto vazio e null contam como a mesma coisa,
 * e 10.0 e 10.00 também (números são comparados pelo valor).
 */
public class AuditDiff {

    /** Valores longos (descrições) são cortados para o log não crescer demais. */
    private static final int MAX_VALUE = 80;

    private final StringJoiner changes = new StringJoiner("; ");
    private int count;

    public AuditDiff field(String name, Object before_change, Object after_change){
        if (!Objects.equals(normalize(before_change), normalize(after_change))) {
            changes.add(name + ": " + show(before_change) + " -> " + show(after_change));
            count++;
        }
        return this;
    }

    /** Campo sigiloso (senha): registra só que mudou, nunca o valor. */
    public AuditDiff secret(String name, boolean changed){
        if (changed) {
            changes.add(name + ": alterada");
            count++;
        }
        return this;
    }

    public boolean hasChanges(){
        return count > 0;
    }

    @Override
    public String toString(){
        return count == 0 ? "sem alterações" : changes.toString();
    }

    private static Object normalize(Object value){
        if (value instanceof String s) {
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
        if (value instanceof BigDecimal n) {
            return n.stripTrailingZeros();
        }
        return value;
    }

    private static String show(Object value){
        Object v = normalize(value);
        if (v == null) {
            return "(vazio)";
        }
        String text = value instanceof BigDecimal n ? n.toPlainString() : v.toString();
        if (text.length() > MAX_VALUE) {
            text = text.substring(0, MAX_VALUE) + "…";
        }
        return v instanceof String ? "\"" + text + "\"" : text;
    }
}
