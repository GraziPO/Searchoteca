package searchoteca.mfa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proteção de dado pessoal (LGPD): o e-mail usado no MFA nunca
 * aparece completo na resposta da API nem nos registros de auditoria.
 */
class EmailMaskerTest {

    @Test
    void deveMascararParteLocalMantendoPrimeiraEUltimaLetraEODominio() {
        // Act
        String mascarado = EmailMasker.mask("joao.silva@umc.br");

        // Assert
        assertEquals("j********a@umc.br", mascarado);
    }

    @ParameterizedTest
    @CsvSource({
            "a@umc.br,   a***@umc.br",   // 1 caractere antes do @
            "ab@umc.br,  a***@umc.br",   // 2 caracteres: fronteira, não revela o segundo
            "abc@umc.br, a*c@umc.br"     // 3 caracteres: primeiro caso com primeira e última letra
    })
    void deveMascararCorretamenteNaFronteiraDoTamanhoDaParteLocal(String email, String esperado) {
        // Act
        String mascarado = EmailMasker.mask(email);

        // Assert
        assertEquals(esperado, mascarado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"joao.umc.br"})
    void deveRetornarApenasAsteriscosQuandoEmailForInvalido(String emailInvalido) {
        // Act
        String mascarado = EmailMasker.mask(emailInvalido);

        // Assert
        assertEquals("***", mascarado);
    }
}
