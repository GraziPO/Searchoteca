package searchoteca.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.DTO.dashboard.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas do dashboard administrativo.
 * Não estende CrudRepository: aqui o resultado são contagens e somas, não entidades.
 * Nomes de tabela, coluna e status seguem o schema.sql da main
 * (CopyStatus, LoanStatus e ExitType gravados com .name()).
 */
@Repository
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cards do topo. :dias define o período das métricas "no período". */
    public DashboardKpis kpis(int days) {
        String sql = """
            SELECT
              (SELECT count(*) FROM org_loans WHERE status = 'OPEN')                              AS abertos,
              (SELECT count(*) FROM org_loans WHERE status = 'OPEN' AND due_date < CURRENT_DATE)  AS atrasados,
              (SELECT count(*) FROM org_students s
                 JOIN org_users u ON u.user_id = s.user_id
                WHERE u.status)                                                                   AS alunos_ativos,
              (SELECT count(*) FROM org_students s
                 JOIN org_users u ON u.user_id = s.user_id
                WHERE u.status
                  AND (s.last_activity IS NULL OR s.last_activity < CURRENT_DATE - 80))           AS alunos_inativos,
              (SELECT count(*) FROM org_copies WHERE status = 'AVAILABLE')                        AS disponiveis,
              (SELECT count(*) FROM org_copies)                                                   AS total_exemplares,
              (SELECT count(*) FROM org_users WHERE must_change_password)                         AS primeiro_acesso,
              (SELECT count(*) FROM org_loans
                WHERE loan_date >= CURRENT_DATE - CAST(:dias AS int))                             AS emprestimos_periodo,
              (SELECT count(*) FROM org_loan_items
                WHERE returned_at >= CURRENT_DATE - CAST(:dias AS int))                           AS devolucoes_periodo,
              (SELECT count(*) FROM org_receipts
                WHERE exit_type <> 'LOAN'
                  AND created_at >= CURRENT_DATE - CAST(:dias AS int))                            AS saidas_periodo,
              (SELECT coalesce(sum(total), 0) FROM org_receipts
                WHERE exit_type = 'SALE'
                  AND created_at >= date_trunc('month', CURRENT_DATE))                            AS vendas_mes,
              (SELECT count(*) FROM org_receipts
                WHERE created_at >= date_trunc('month', CURRENT_DATE))                            AS comprovantes_mes
            """;

        return jdbc.queryForObject(sql, Map.of("dias", days), (rs, i) -> new DashboardKpis(
                rs.getLong("abertos"),
                rs.getLong("atrasados"),
                rs.getLong("alunos_ativos"),
                rs.getLong("alunos_inativos"),
                rs.getLong("disponiveis"),
                rs.getLong("total_exemplares"),
                rs.getLong("primeiro_acesso"),
                rs.getLong("emprestimos_periodo"),
                rs.getLong("devolucoes_periodo"),
                rs.getLong("saidas_periodo"),
                rs.getBigDecimal("vendas_mes") == null ? BigDecimal.ZERO : rs.getBigDecimal("vendas_mes"),
                rs.getLong("comprovantes_mes")));
    }

    /** Gráfico de linhas: um ponto por dia, inclusive dias sem movimento. */
    public List<Diary> diary(int days) {
        String sql = """
            SELECT d::date AS dia,
                   (SELECT count(*) FROM org_loans l
                     WHERE l.loan_date::date = d::date)        AS emprestimos,
                   (SELECT count(*) FROM org_loan_items li
                     WHERE li.returned_at::date = d::date)     AS devolucoes
            FROM generate_series(CURRENT_DATE - CAST(:dias AS int) + 1,
                                 CURRENT_DATE,
                                 interval '1 day') AS d
            ORDER BY dia
            """;
        return jdbc.query(sql, Map.of("dias", days), (rs, i) -> new Diary(
                rs.getObject("dia", LocalDate.class),
                rs.getLong("emprestimos"),
                rs.getLong("devolucoes")));
    }

    /** Rosca: exemplares por status (AVAILABLE, LOANED, SOLD, DONATED, WRITTEN_OFF). */
    public List<CountByStatus> copiesByStatus() {
        String sql = """
            SELECT status, count(*) AS quantidade
            FROM org_copies
            GROUP BY status
            ORDER BY quantidade DESC
            """;
        return jdbc.query(sql, Map.of(), (rs, i) -> new CountByStatus(
                rs.getString("status"),
                rs.getLong("quantidade")));
    }

    /** Barras: % dos exemplares de cada departamento que estão emprestados agora. */
    public List<DepartmentOccupation> departmentOccupation() {
        String sql = """
            SELECT d.depart_name                                   AS departamento,
                   count(c.copy_id) FILTER (WHERE c.status = 'LOANED') AS emprestados,
                   count(c.copy_id)                                AS total
            FROM org_depart d
            LEFT JOIN org_copies c ON c.depart_code = d.depart_code
            GROUP BY d.depart_name
            ORDER BY emprestados DESC, d.depart_name
            """;
        return jdbc.query(sql, Map.of(), (rs, i) -> new DepartmentOccupation(
                rs.getString("departamento"),
                rs.getLong("emprestados"),
                rs.getLong("total")));
    }

    /** Tabela: empréstimos vencidos, mais antigos primeiro, só com itens ainda não devolvidos. */
    public List<DelayedLoan> delayed(int limit) {
        String sql = """
            SELECT l.loan_id,
                   u.complete_name                   AS aluno,
                   s.ra                              AS ra,
                   string_agg(b.title, ', ' ORDER BY b.title) AS exemplares,
                   l.due_date,
                   CURRENT_DATE - l.due_date         AS dias_atraso
            FROM org_loans l
            JOIN org_students s     ON s.student_id = l.student_id
            JOIN org_users u        ON u.user_id    = s.user_id
            JOIN org_loan_items li  ON li.loan_id   = l.loan_id AND li.returned_at IS NULL
            JOIN org_copies c       ON c.copy_id    = li.copy_id
            JOIN org_books b        ON b.isbn       = c.isbn
            WHERE l.status = 'OPEN'
              AND l.due_date < CURRENT_DATE
            GROUP BY l.loan_id, u.complete_name, s.ra, l.due_date
            ORDER BY l.due_date
            LIMIT :limite
            """;
        return jdbc.query(sql, Map.of("limite", limit), (rs, i) -> new DelayedLoan(
                rs.getLong("loan_id"),
                rs.getString("aluno"),
                rs.getString("ra"),
                rs.getString("exemplares"),
                rs.getObject("due_date", LocalDate.class),
                rs.getLong("dias_atraso")));
    }

    /** Painel de segurança: eventos da trilha de auditoria nas últimas 24 h. */
    public SecuritySummary security() {
        String sql = """
            SELECT
              count(*) FILTER (WHERE action = 'LOGIN_SUCCESS')          AS logins_ok,
              count(*) FILTER (WHERE action = 'LOGIN_FAILURE')          AS logins_falha,
              count(*) FILTER (WHERE action IN ('MFA_FAILURE','MFA_LOCKED')) AS mfa_falha,
              count(*) FILTER (WHERE action = 'ACCESS_DENIED')          AS acesso_negado,
              count(*) FILTER (WHERE action = 'PASSWORD_RESET_SUCCESS') AS senhas_redefinidas
            FROM auditTrail_logs
            WHERE time_stamp >= now() - interval '24 hours'
            """;
        return jdbc.queryForObject(sql, Map.of(), (rs, i) -> new SecuritySummary(
                rs.getLong("logins_ok"),
                rs.getLong("logins_falha"),
                rs.getLong("mfa_falha"),
                rs.getLong("acesso_negado"),
                rs.getLong("senhas_redefinidas")));
    }
}
