package searchoteca.repository.archive;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/** Consultas das regras de empréstimo: limite de livros por aluno e bloqueio por atraso. */
@Repository
public class LoanRulesRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LoanRulesRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Trava a linha do aluno até o fim da transação. Sem isso, dois operadores
     * emprestando ao mesmo aluno ao mesmo tempo poderiam, juntos, passar do limite.
     */
    public void lockStudent(Long studentId) {
        jdbc.queryForList("SELECT student_id FROM org_students WHERE student_id = :sid FOR UPDATE",
                Map.of("sid", studentId), Long.class);
    }

    /** Quantos exemplares o aluno está com ele agora (itens ainda não devolvidos). */
    public int countOpenItems(Long studentId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM org_loan_items i
                JOIN org_loans l ON l.loan_id = i.loan_id
                WHERE l.student_id = :sid AND l.status = 'OPEN' AND i.returned_at IS NULL
                """, Map.of("sid", studentId), Integer.class);
        return n == null ? 0 : n;
    }

    /** O aluno tem algum livro com a devolução vencida (prazo antes de hoje e ainda não devolvido)? */
    public boolean hasOverdueItems(Long studentId) {
        Boolean overdue = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM org_loan_items i
                    JOIN org_loans l ON l.loan_id = i.loan_id
                    WHERE l.student_id = :sid AND l.status = 'OPEN'
                      AND i.returned_at IS NULL
                      AND l.due_date < CURRENT_DATE)
                """, Map.of("sid", studentId), Boolean.class);
        return Boolean.TRUE.equals(overdue);
    }
}
