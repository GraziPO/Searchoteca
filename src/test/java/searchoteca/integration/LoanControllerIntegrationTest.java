package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.model.LoanItemModel;
import searchoteca.repository.archive.LoanItemRepository;
import searchoteca.repository.archive.LoanRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.users.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoanController (/api/emprestimos): RF08 e RF10 - empréstimo, cancelamento e devolução,
 * com o status de cada exemplar atualizado a cada movimentação.
 */
class LoanControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/emprestimos";

    @Autowired
    private CopyRepository copyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanItemRepository loanItemRepository;

    @Autowired
    private StudentRepository studentRepository;

    private String tokenOperador() {
        return tokenDoPerfil("loan_op");
    }

    private ResponseEntity<Map<String, Object>> emprestar(String ra, List<Long> exemplares) {
        return post(BASE, tokenOperador(), Map.of("ra", ra, "items", exemplares));
    }

    private long criarEmprestimo(String ra, List<Long> exemplares) {
        ResponseEntity<Map<String, Object>> resposta = emprestar(ra, exemplares);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return comoLong(resposta.getBody().get("loanId"));
    }

    private String statusDoExemplar(Long copyId) {
        return copyRepository.findByCopyId(copyId).getStatus();
    }

    // ----------------------------------------------------------------- POST /api/emprestimos

    @Test
    void create_comAlunoEExemplarDisponivel_deveRetornar200EMarcarExemplarComoEmprestado() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);
        Aluno aluno = criarAluno();

        // Act
        ResponseEntity<Map<String, Object>> resposta = emprestar(aluno.ra(), List.of(exemplar));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("status", "OPEN")
                .containsEntry("dueDate", LocalDate.now().plusDays(15).toString());
        assertThat(resposta.getBody().get("loanId")).isNotNull();
        assertThat(comoLong(resposta.getBody().get("studentId")))
                .isEqualTo(studentRepository.findByRa(aluno.ra()).getStudentId());
        assertThat(comoLong(resposta.getBody().get("operatorId")))
                .isEqualTo(usuarioDoPerfil("loan_op").id());
        assertThat(statusDoExemplar(exemplar)).isEqualTo("LOANED");
    }

    @Test
    void create_comRaInexistente_deveRetornar404ComMensagem() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta = emprestar("RA-INEXISTENTE-" + uid(), List.of(exemplar));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "Aluno não encontrado");
        assertThat(statusDoExemplar(exemplar)).isEqualTo("AVAILABLE");
    }

    @Test
    void create_comExemplarJaEmprestado_deveRetornar409EDesfazerOEmprestimo() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);
        criarEmprestimo(criarAluno().ra(), List.of(exemplar));
        Aluno outroAluno = criarAluno();

        // Act
        ResponseEntity<Map<String, Object>> resposta = emprestar(outroAluno.ra(), List.of(exemplar));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", "Esemplar não disponivel");
        Long idOutroAluno = studentRepository.findByRa(outroAluno.ra()).getStudentId();
        assertThat(loanRepository.findByStudentId(idOutroAluno)).isEmpty();   // transação desfeita
    }

    @Test
    void create_comOperadorDeAcervo_deveRetornar403ComMensagem() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);
        Aluno aluno = criarAluno();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenDoPerfil("warehouse_op"),
                Map.of("ra", aluno.ra(), "items", List.of(exemplar)));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
        assertThat(statusDoExemplar(exemplar)).isEqualTo("AVAILABLE");
    }

    // ----------------------------------------------------------------- GET /api/emprestimos

    @Test
    void findAll_deveRetornar200ComEmprestimoCriadoNaLista() {
        // Arrange
        long loanId = criarEmprestimo(criarAluno().ra(), criarExemplaresDeUmLivroNovo(1));

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenOperador());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().stream().map(e -> comoLong(e.get("loanId"))).toList()).contains(loanId);
    }

    // ----------------------------------------------------------------- GET /api/emprestimos/{id}

    @Test
    void findById_comIdExistente_deveRetornar200ComDadosDoEmprestimo() {
        // Arrange
        Aluno aluno = criarAluno();
        long loanId = criarEmprestimo(aluno.ra(), criarExemplaresDeUmLivroNovo(1));

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + loanId, tokenOperador());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(comoLong(resposta.getBody().get("loanId"))).isEqualTo(loanId);
        assertThat(comoLong(resposta.getBody().get("studentId")))
                .isEqualTo(studentRepository.findByRa(aluno.ra()).getStudentId());
        assertThat(resposta.getBody()).containsEntry("status", "OPEN");
        assertThat(resposta.getBody().get("loanDate")).isNotNull();
    }

    @Test
    void findById_comIdInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/999999999", tokenOperador());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Empréstimo não encontrado");
    }

    // ----------------------------------------------------------------- GET /api/emprestimos/historico

    @Test
    void findOwn_comTokenDoAluno_deveRetornarSomenteOsEmprestimosDoProprioAluno() {
        // Arrange
        Aluno aluno = criarAluno();
        Aluno outroAluno = criarAluno();
        long emprestimoDoAluno = criarEmprestimo(aluno.ra(), criarExemplaresDeUmLivroNovo(1));
        criarEmprestimo(outroAluno.ra(), criarExemplaresDeUmLivroNovo(1));
        String tokenAluno = login(aluno.username(), aluno.senha());

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE + "/historico", tokenAluno);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().stream().map(e -> comoLong(e.get("loanId"))).toList())
                .containsExactly(emprestimoDoAluno);
    }

    @Test
    void findOwn_comOperadorDeEmprestimo_deveRetornar403ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/historico", tokenOperador());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
    }

    // ----------------------------------------------------------------- POST /api/emprestimos/{id}/cancelar

    @Test
    void cancel_comEmprestimoAberto_deveRetornar200ComStatusCanceladoELiberarExemplar() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);
        long loanId = criarEmprestimo(criarAluno().ra(), List.of(exemplar));

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE + "/" + loanId + "/cancelar", tokenOperador(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "CANCELLED");
        assertThat(loanRepository.findByLoanId(loanId).getStatus()).isEqualTo("CANCELLED");
        assertThat(statusDoExemplar(exemplar)).isEqualTo("AVAILABLE");
    }

    @Test
    void cancel_comEmprestimoJaCancelado_deveRetornar409ComMensagem() {
        // Arrange
        long loanId = criarEmprestimo(criarAluno().ra(), criarExemplaresDeUmLivroNovo(1));
        post(BASE + "/" + loanId + "/cancelar", tokenOperador(), null);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE + "/" + loanId + "/cancelar", tokenOperador(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "impossível realizar operação. Verifique o status do pedido.");
    }

    // ----------------------------------------------------------------- POST /api/emprestimos/devolucao/{copyId}

    @Test
    void returnItem_doUnicoExemplar_deveRetornar200FecharEmprestimoELiberarExemplar() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);
        long loanId = criarEmprestimo(criarAluno().ra(), List.of(exemplar));

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE + "/devolucao/" + exemplar, tokenOperador(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "CLOSED");
        assertThat(comoLong(resposta.getBody().get("loanId"))).isEqualTo(loanId);
        assertThat(statusDoExemplar(exemplar)).isEqualTo("AVAILABLE");

        List<LoanItemModel> itens = loanItemRepository.findByLoanId(loanId);
        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).getReturned_at()).isNotNull();
        assertThat(itens.get(0).getReturned_to()).isEqualTo(usuarioDoPerfil("loan_op").id());
    }

    @Test
    void returnItem_deUmDeDoisExemplares_deveManterEmprestimoAberto() {
        // Arrange
        List<Long> exemplares = criarExemplaresDeUmLivroNovo(2);
        long loanId = criarEmprestimo(criarAluno().ra(), exemplares);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE + "/devolucao/" + exemplares.get(0), tokenOperador(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "OPEN");
        assertThat(loanRepository.findByLoanId(loanId).getStatus()).isEqualTo("OPEN");
        assertThat(statusDoExemplar(exemplares.get(0))).isEqualTo("AVAILABLE");
        assertThat(statusDoExemplar(exemplares.get(1))).isEqualTo("LOANED");
    }

    @Test
    void returnItem_comExemplarSemEmprestimo_deveRetornar409ComMensagem() {
        // Arrange
        Long exemplar = criarExemplaresDeUmLivroNovo(1).get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE + "/devolucao/" + exemplar, tokenOperador(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "Não existe registro de empréstimo relacionado a esta cópia");
    }
}
