package searchoteca.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.CopyModel;
import searchoteca.model.DepartmentModel;
import searchoteca.model.LocationModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;
import searchoteca.repository.DepartmentRepository;
import searchoteca.repository.LocationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RF01 / RF02 - Departamentos: edição e exclusão permitida
 * somente quando não há localizações ou exemplares vinculados.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock BookRepository bookRepository;
    @Mock LocationRepository locationRepository;
    @Mock CopyRepository copyRepository;

    @InjectMocks DepartmentService departmentService;

    private DepartmentModel departamento(String codigo, String nome, String descricao) {
        DepartmentModel d = new DepartmentModel();
        d.setDepartCode(codigo);
        d.setDepartName(nome);
        d.setDepartDesc(descricao);
        return d;
    }

    @Test
    void deveExcluirDepartamentoQuandoNaoHaLocalizacoesNemExemplaresVinculados() {
        // Arrange
        DepartmentModel dep = departamento("DEP01", "Literatura", "Livros Aa - Ac");
        when(departmentRepository.findByDepartCode("DEP01")).thenReturn(dep);
        when(locationRepository.findByDepartCode("DEP01")).thenReturn(List.of());
        when(copyRepository.findByDepartCode("DEP01")).thenReturn(List.of());

        // Act
        departmentService.delete("DEP01");

        // Assert
        verify(departmentRepository).delete(dep);
    }

    @Test
    void deveLancarConflitoAoExcluirDepartamentoComLocalizacoesVinculadas() {
        // Arrange
        when(departmentRepository.findByDepartCode("DEP01"))
                .thenReturn(departamento("DEP01", "Literatura", "Livros Aa - Ac"));
        when(locationRepository.findByDepartCode("DEP01")).thenReturn(List.of(new LocationModel()));

        // Act
        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> departmentService.delete("DEP01"));

        // Assert
        assertEquals("Este departamento não pode ser deletado, pois há localizações ou exemplares ligados a ele",
                ex.getMessage());
        verify(departmentRepository, never()).delete(any(DepartmentModel.class));
    }

    @Test
    void deveLancarConflitoAoExcluirDepartamentoSemLocalizacoesMasComUmUnicoExemplar() {
        // Arrange - fronteira: nenhuma localização, apenas 1 exemplar ligado ao departamento
        when(departmentRepository.findByDepartCode("DEP01"))
                .thenReturn(departamento("DEP01", "Literatura", "Livros Aa - Ac"));
        when(locationRepository.findByDepartCode("DEP01")).thenReturn(List.of());
        when(copyRepository.findByDepartCode("DEP01")).thenReturn(List.of(new CopyModel()));

        // Act
        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> departmentService.delete("DEP01"));

        // Assert
        assertTrue(ex.getMessage().contains("exemplares"));
        verify(departmentRepository, never()).delete(any(DepartmentModel.class));
    }

    @Test
    void deveLancarNaoEncontradoAoExcluirDepartamentoComCodigoNuloSemConsultarBanco() {
        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> departmentService.delete(null));

        // Assert
        assertEquals("Departamento não existente", ex.getMessage());
        verifyNoInteractions(departmentRepository, locationRepository, copyRepository);
    }

    @Test
    void deveAtualizarNomeEDescricaoMantendoOCodigoDoDepartamento() {
        // Arrange - o corpo tenta trocar o código, mas o código é a chave única e não muda
        DepartmentModel existente = departamento("DEP01", "Literatura", "Livros Aa - Ac");
        DepartmentModel novosDados = departamento("OUTRO", "Literatura Brasileira", "Livros Ad - Af");
        when(departmentRepository.findByDepartCode("DEP01")).thenReturn(existente);
        when(departmentRepository.save(any(DepartmentModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        DepartmentModel atualizado = departmentService.update("DEP01", novosDados);

        // Assert
        assertEquals("Literatura Brasileira", atualizado.getDepartName());
        assertEquals("Livros Ad - Af", atualizado.getDepartDesc());
        assertEquals("DEP01", atualizado.getDepartCode());
    }

    @Test
    void deveLancarNaoEncontradoAoAtualizarDepartamentoInexistente() {
        // Arrange
        when(departmentRepository.findByDepartCode("NAOEXISTE")).thenReturn(null);

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> departmentService.update("NAOEXISTE", new DepartmentModel()));

        // Assert
        assertEquals("Departamento inexistente", ex.getMessage());
        verify(departmentRepository, never()).save(any(DepartmentModel.class));
    }
}
