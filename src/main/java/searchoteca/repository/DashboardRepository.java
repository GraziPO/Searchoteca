package searchoteca.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.DTO.dashboard.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository{
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardKpis kpis(){
        String query = """
                SELECT
                (SELECT count(*) FROM org_loans WHERE status = 'OPEN')                              AS abertos,
                (SELECT count(*) FROM org_loans WHERE status = 'OPEN' AND due_date < CURRENT_DATE)  AS atrasados,
                (SELECT count(*) FROM org_users WHERE role_code = 'students' AND status = 'ACTIVE') AS alunos,
                (SELECT count(*) FROM org_copies WHERE status = 'AVAILABLE')                        AS disponivel,
                (SELECT count(*) FROM org_copies)                                                   AS total,
                (SELECT count(*) FROM org_users WHERE must_change_password)                         AS primeiro_acesso
                """;

        return jdbc.queryForObject(query, Map.of(), (rs, i) -> new DashboardKpis(
                rs.getLong("abertos"),
                rs.getLong("atrasados"),
                rs.getLong("alunos"),
                rs.getLong("disponiveis"),
                rs.getLong("total"),
                rs.getLong("primeiro_acesso")));
    }

    public List<Diary> diary(int days) {
        String sql = """
                SELECT d::date AS dia,
                          (SELECT count(*) FROM org_loans l WHERE l.loan_date::date = d::date)   AS emprestimos,
                          (SELECT count(*) FROM org_loans l WHERE l.return_date::date = d::date) AS devolucoes
                   FROM generate_series(CURRENT_DATE - :dias + 1, CURRENT_DATE, interval '1 day') AS d
                   ORDER BY dia
                """;
        return jdbc.query(sql, Map.of("dias", days), (rs, i) -> new Diary(
                rs.getObject("dia", LocalDate.class),
                rs.getLong("emprestimos"),
                rs.getLong("devolucoes")));
    }

    public List<CountByStatus> copyByStatus(int days) {
        String sql = """
            SELECT status, count(*) AS quantidade
            FROM org_copies
            GROUP BY status
            ORDER BY quantidade DESC
            """;
        return jdbc.query(sql, Map.of(), (rs, i) -> new CountByStatus(
                rs.getString("status"), rs.getLong("quantidade")));
    }

    public List<DepartmentOccupation> DepartmentOccupation(int days) {
        String sql = """
            SELECT d.depart_name AS departamento,
                   count(c.*) FILTER (WHERE c.status = 'LOANED') AS emprestados,
                   count(c.*)                                    AS total
            FROM org_depart d
            LEFT JOIN org_copies c ON c.depart_id = d.depart_id
            GROUP BY d.depart_name
            ORDER BY emprestados DESC
            """;
        return jdbc.query(sql, Map.of(), (rs, i) -> new DepartmentOccupation(
                rs.getString("departamento"),
                rs.getLong("emprestados"),
                rs.getLong("total")));
    }

    public List<DelayedLoan> delayed(int limite) {
        String sql = """
            SELECT l.loan_id,
                   u.complete_name                    AS aluno,
                   string_agg(b.title, ', ')          AS exemplar,
                   l.due_date,
                   CURRENT_DATE - l.due_date::date    AS dias_atraso
            FROM org_loans l
            JOIN org_users u        ON u.user_id = l.student_id
            JOIN org_loan_items li  ON li.loan_id = l.loan_id
            JOIN org_copies c       ON c.copy_id = li.copy_id
            JOIN org_books b        ON b.isbn = c.isbn
            WHERE l.status = 'OPEN' AND l.due_date < CURRENT_DATE
            GROUP BY l.loan_id, u.complete_name, l.due_date
            ORDER BY l.due_date
            LIMIT :limite
            """;
        return jdbc.query(sql, Map.of("limite", limite), (rs, i) -> new DelayedLoan(
                rs.getLong("loan_id"),
                rs.getString("aluno"),
                rs.getString("exemplar"),
                rs.getObject("due_date", LocalDate.class),
                rs.getLong("dias_atraso")));
    }
}
