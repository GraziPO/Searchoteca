package searchoteca.auditTrail;

import java.util.Objects;
import java.util.StringJoiner;

public class AuditDiff {
    private final StringJoiner changes = new StringJoiner("; ");

    public AuditDiff field (String name, Object before_change, Object after_change){
        if (!Objects.equals(before_change, after_change)) {
            changes.add(name + ": "+ before_change + " -> " + after_change);
        }
        return this;
    }

    @Override
    public String toString(){
        return changes.length() == 0 ? "sem alterações" : changes.toString();}
}
