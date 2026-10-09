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
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;
import searchoteca.repository.LocationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RF03 / RF04 - Localizações: código único, exclusão permitida somente
 * sem exemplares vinculados e consulta dos livros guardados na localização.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock LocationRepository locationRepository;
    @Mock BookRepository bookRepository;
    @Mock CopyRepository copyRepository;

    @InjectMocks LocationService locationService;

    private LocationModel localizacao(String codigo) {
        LocationModel l = new LocationModel();
        l.setLocalCode(codigo);
        l.setLocalName("Prateleira " + codigo);
        l.setDepartCode("DEP01");
        return l;
    }

    private CopyModel exemplar(String isbn) {
        CopyModel c = new CopyModel();
        c.setIsbn(isbn);
        c.setLocalCode("LOC01");
        return c;
    }

    private BookModel livro(String isbn) {
        BookModel b = new BookModel();
        b.setIsbn(isbn);
        return b;
    }

    @Test
    void deveCriarLocalizacaoQuandoCodigoAindaNaoExiste() {
        // Arrange
        LocationModel nova = localizacao("LOC01");
        when(locationRepository.existsByLocalCode("LOC01")).thenReturn(false);
        when(locationRepository.save(nova)).thenReturn(nova);

        // Act
        LocationModel criada = locationService.create(nova);

        // Assert
        assertEquals("LOC01", criada.getLocalCode());
        verify(locationRepository).save(nova);
    }

    @Test
    void deveLancarConflitoAoCriarLocalizacaoComCodigoDuplicado() {
        // Arrange
        when(locationRepository.existsByLocalCode("LOC01")).thenReturn(true);

        // Act
        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> locationService.create(localizacao("LOC01")));

        // Assert
        assertEquals("Já existe uma localização com esse código.", ex.getMessage());
        verify(locationRepository, never()).save(any(LocationModel.class));
    }

    @Test
    void deveExcluirLocalizacaoQuandoNaoHaExemplaresGuardados() {
        // Arrange
        when(locationRepository.findByLocalCode("LOC01")).thenReturn(localizacao("LOC01"));
        when(copyRepository.findByLocalCode("LOC01")).thenReturn(List.of());

        // Act
        locationService.delete("LOC01");

        // Assert
        verify(locationRepository).deleteByLocalCode("LOC01");
    }

    @Test
    void deveLancarConflitoAoExcluirLocalizacaoComExemplaresGuardados() {
        // Arrange
        when(locationRepository.findByLocalCode("LOC01")).thenReturn(localizacao("LOC01"));
        when(copyRepository.findByLocalCode("LOC01")).thenReturn(List.of(exemplar("9788535914849")));

        // Act
        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> locationService.delete("LOC01"));

        // Assert
        assertEquals("Não é possível excluir: há exemplares guardados nesta localização.", ex.getMessage());
        verify(locationRepository, never()).deleteByLocalCode(anyString());
    }

    @Test
    void deveLancarNaoEncontradoAoExcluirLocalizacaoInexistente() {
        // Arrange
        when(locationRepository.findByLocalCode("LOC99")).thenReturn(null);

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> locationService.delete("LOC99"));

        // Assert
        assertEquals("registro não encontrado", ex.getMessage());
        verifyNoInteractions(copyRepository);
        verify(locationRepository, never()).deleteByLocalCode(anyString());
    }

    @Test
    void deveRetornarCadaLivroUmaUnicaVezQuandoHaVariosExemplaresDoMesmoIsbnNaLocalizacao() {
        // Arrange - 3 exemplares do mesmo livro + 1 exemplar de outro livro
        when(copyRepository.findByLocalCode("LOC01")).thenReturn(List.of(
                exemplar("9788535914849"), exemplar("9788535914849"),
                exemplar("9788535914849"), exemplar("9788525432186")));
        when(bookRepository.findByIsbn("9788535914849")).thenReturn(livro("9788535914849"));
        when(bookRepository.findByIsbn("9788525432186")).thenReturn(livro("9788525432186"));

        // Act
        List<BookModel> livros = locationService.findBookByLocalCode("LOC01");

        // Assert
        assertEquals(2, livros.size());
        assertEquals("9788535914849", livros.get(0).getIsbn());
        assertEquals("9788525432186", livros.get(1).getIsbn());
        verify(bookRepository, times(1)).findByIsbn("9788535914849");
    }

    @Test
    void deveRetornarListaVaziaQuandoLocalizacaoNaoTemExemplares() {
        // Arrange
        when(copyRepository.findByLocalCode("LOC01")).thenReturn(List.of());

        // Act
        List<BookModel> livros = locationService.findBookByLocalCode("LOC01");

        // Assert
        assertTrue(livros.isEmpty());
        verifyNoInteractions(bookRepository);
    }
}
