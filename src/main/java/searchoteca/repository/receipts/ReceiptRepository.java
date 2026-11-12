package searchoteca.repository.receipts;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.model.Books.status.ExitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReceiptRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ReceiptRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long nextId(){
        return jdbc.getJdbcTemplate().queryForObject("select nextval('receipt_seq')",Long.class);
    }

    public void insert(long id, String code, ExitType type, Long operatorId, Long loanId,
                       BigDecimal total, LocalDateTime createdAt, String snapshotJson){
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("code", code)
                .addValue("type", type.name())
                .addValue("operatorId", operatorId)
                .addValue("loanId", loanId)
                .addValue("total", total)
                .addValue("createdAt", createdAt)
                .addValue("snapshot", snapshotJson);

        jdbc.update("""
                INSERT INTO org_receipts
                    (receipt_id, receipt_code, exit_type, operator_id, loan_id, total, created_at, snapshot)
                VALUES (:id, :code, :type, :operatorId, :loanId, :total, :createdAt, CAST(:snapshot AS jsonb))
                """, params);
    }

    public void insertItem(long receiptId, Long copyId, BigDecimal unitPrice){
        jdbc.update("INSERT INTO org_receipt_items (receipt_id, copy_id, unit_price) VALUES (:r, :c, :p)",
        new MapSqlParameterSource()
                .addValue("r", receiptId)
                .addValue("c", copyId)
                .addValue("p", unitPrice));
    }

    public Optional<String> findSnapshotByCode(String code){
        return jdbc.query("SELECT snapshot::text FROM org_receipts WHERE receipt_code = :code",
                Map.of("code", code), (rs,i) -> rs.getString(1)).stream().findFirst();
    }
}
