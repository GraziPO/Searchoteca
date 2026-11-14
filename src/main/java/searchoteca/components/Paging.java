package searchoteca.components;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Paginação das listagens (RNF06). Todas usam os mesmos parâmetros:
 *   ?page=0&size=20   (page começa em 0)
 * Valores fora da faixa são corrigidos em vez de dar erro: página negativa vira 0,
 * e o tamanho fica entre 1 e 100.
 */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {}

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
