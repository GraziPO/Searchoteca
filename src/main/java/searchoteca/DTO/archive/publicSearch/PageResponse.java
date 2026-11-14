package searchoteca.DTO.archive.publicSearch;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato único das respostas paginadas (busca pública e listagens internas).
 *
 *   { "content": [...], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /** Converte a página do Spring Data para o formato da API (sem expor os detalhes internos do Page). */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
