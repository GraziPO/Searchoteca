package searchoteca.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.CopyModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RF05 / RF06 - Registro-base de livros (ISBN) e vínculo com exemplares.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock CopyRepository copyRepository;

    @InjectMocks BookService bookService;

    private BookModel livro(String isbn, String titulo, Integer ano) {
        BookModel b = new BookModel();
        b.setIsbn(isbn);
        b.setTitle(titulo);
        b.setAuthor("Machado de Assis");
        b.setRel_year(ano);
        b.setPublisher("Penguin");
        b.setGenre("Romance");
        return b;
    }

    @Test
    void deveExcluirLivroQuandoNaoHaExemplaresCadastrados() {
        // Arrange
        when(bookRepository.findByIsbn("9788535914849"))
                .thenReturn(livro("9788535914849", "Dom Casmurro", 1899));
        when(copyRepository.findByIsbn("9788535914849")).thenReturn(List.of());

        // Act
        bookService.delete("9788535914849");

        // Assert
        verify(bookRepository).deleteByIsbn("9788535914849");
    }

    @Test
    void deveLancarConflitoAoExcluirLivroComExemplaresCadastrados() {
        // Arrange
        when(bookRepository.findByIsbn("9788535914849"))
                .thenReturn(livro("9788535914849", "Dom Casmurro", 1899));
        when(copyRepository.findByIsbn("9788535914849")).thenReturn(List.of(new CopyModel()));

        // Act
        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> bookService.delete("9788535914849"));

        // Assert
        assertEquals("Não é possível excluir: este livro tem exemplares cadastrados.", ex.getMessage());
        verify(bookRepository, never()).deleteByIsbn(anyString());
    }

    @Test
    void deveLancarNaoEncontradoAoExcluirLivroInexistente() {
        // Arrange
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(null);

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookService.delete("0000000000000"));

        // Assert
        assertEquals("Nenhum livro encontrado", ex.getMessage());
        verifyNoInteractions(copyRepository);
    }

    @Test
    void deveAtualizarDadosDoLivroMantendoOIsbnOriginal() {
        // Arrange - o corpo traz outro ISBN, mas o ISBN identifica o registro-base e não muda
        BookModel existente = livro("9788535914849", "Dom Casmurro", 1899);
        BookModel novosDados = livro("1111111111111", "Dom Casmurro - Edição Comentada", 2016);
        when(bookRepository.findByIsbn("9788535914849")).thenReturn(existente);
        when(bookRepository.save(any(BookModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        BookModel atualizado = bookService.update("9788535914849", novosDados);

        // Assert
        assertEquals("Dom Casmurro - Edição Comentada", atualizado.getTitle());
        assertEquals(2016, atualizado.getRel_year());
        assertEquals("9788535914849", atualizado.getIsbn());
    }

    @Test
    void deveAceitarAnoDeLancamentoEmBrancoNaAtualizacao() {
        // Arrange - ano de lançamento é opcional no cadastro
        BookModel existente = livro("9788535914849", "Dom Casmurro", 1899);
        when(bookRepository.findByIsbn("9788535914849")).thenReturn(existente);
        when(bookRepository.save(any(BookModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        BookModel atualizado = bookService.update("9788535914849",
                livro("9788535914849", "Dom Casmurro", null));

        // Assert
        assertNull(atualizado.getRel_year());
        verify(bookRepository).save(existente);
    }

    @Test
    void deveLancarNaoEncontradoAoAtualizarLivroInexistente() {
        // Arrange
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(null);

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookService.update("0000000000000", new BookModel()));

        // Assert
        assertEquals("registro não encontrado", ex.getMessage());
        verify(bookRepository, never()).save(any(BookModel.class));
    }

    @Test
    void deveLancarNaoEncontradoAoBuscarLivroComIsbnNuloSemConsultarBanco() {
        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookService.findByIsbn(null));

        // Assert
        assertEquals("Nenhum livro encontrado", ex.getMessage());
        verifyNoInteractions(bookRepository);
    }
}
