package searchoteca.service.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import searchoteca.DTO.archive.IsbnLookupResult;
import searchoteca.components.Isbn;
import searchoteca.exception.ExternalServiceException;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.repository.catalog.BookRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Busca os dados de um livro pelo ISBN na Open Library (RF05 / RNF09).
 *
 *   GET https://openlibrary.org/api/books?bibkeys=ISBN:<isbn>&format=json&jscmd=data
 *
 * Só preenche o formulário: nada é salvo aqui. Se a Open Library não conhecer o ISBN,
 * ou estiver fora do ar, o operador cadastra manualmente, como antes.
 */
@Service
public class IsbnLookupService {

    private static final Logger log = LoggerFactory.getLogger(IsbnLookupService.class);
    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");

    /** "Assuntos" da Open Library que não são gênero (marcações de acervo digital). */
    private static final Set<String> NOT_A_GENRE = Set.of(
            "accessible book", "protected daisy", "in library", "lending library", "large type books",
            "open library staff picks", "overdrive");

    // tamanhos das colunas de org_books: o que vier maior é cortado para caber
    private static final int MAX_TITLE = 100;
    private static final int MAX_AUTHOR = 100;
    private static final int MAX_PUBLISHER = 50;
    private static final int MAX_GENRE = 50;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final BookRepository bookRepository;

    public IsbnLookupService(JsonMapper jsonMapper,
                             BookRepository bookRepository,
                             @Value("${isbn-lookup.base-url:https://openlibrary.org}") String baseUrl,
                             @Value("${isbn-lookup.connect-timeout-ms:3000}") long connectTimeoutMs,
                             @Value("${isbn-lookup.read-timeout-ms:5000}") long readTimeoutMs) {
        this.jsonMapper = jsonMapper;
        this.bookRepository = bookRepository;

        // tempo máximo de espera: com a Open Library lenta, o operador não fica preso no formulário
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                // a Open Library pede que as aplicações se identifiquem
                .defaultHeader("User-Agent", "Searchoteca/1.0 (sistema de biblioteca - PFC)")
                .build();
    }

    public IsbnLookupResult lookup(String rawIsbn) {
        String isbn = Isbn.normalize(rawIsbn);
        if (!Isbn.isValid(isbn)) {
            throw new ResourceConflictException("ISBN inválido. Confira os números (ISBN-10 ou ISBN-13).");
        }
        if (bookRepository.findByIsbn(isbn) != null) {
            throw new ResourceConflictException("Este ISBN já está cadastrado no acervo.");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/api/books")
                            .queryParam("bibkeys", "ISBN:" + isbn)
                            .queryParam("format", "json")
                            .queryParam("jscmd", "data")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("Open Library indisponível para o ISBN {}: {}", isbn, e.getMessage());
            throw new ExternalServiceException(
                    "Não foi possível consultar a Open Library agora. Preencha os dados do livro manualmente.");
        }

        OpenLibraryBook book = parse(isbn, body);
        if (book == null || isBlank(book.title())) {
            throw new ResourceNotFoundException(
                    "ISBN não encontrado na Open Library. Preencha os dados do livro manualmente.");
        }

        return new IsbnLookupResult(
                isbn,
                cut(book.title(), MAX_TITLE),
                cut(joinNames(book.authors()), MAX_AUTHOR),
                year(book.publishDate()),
                cut(firstName(book.publishers()), MAX_PUBLISHER),
                cut(genre(book.subjects()), MAX_GENRE),
                "Open Library");
    }

    // ===================================================================== leitura da resposta

    /** A resposta é { "ISBN:<isbn>": { ...dados... } }, ou {} quando o ISBN não existe lá. */
    private OpenLibraryBook parse(String isbn, String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = jsonMapper.readTree(body);
            JsonNode entry = root.get("ISBN:" + isbn);
            if (entry == null || entry.isNull()) {
                return null;
            }
            return jsonMapper.treeToValue(entry, OpenLibraryBook.class);
        } catch (RuntimeException e) {
            log.warn("Resposta inesperada da Open Library para o ISBN {}: {}", isbn, e.getMessage());
            throw new ExternalServiceException(
                    "A Open Library respondeu num formato inesperado. Preencha os dados do livro manualmente.");
        }
    }

    private static String joinNames(List<Named> list) {
        if (list == null || list.isEmpty()) return null;
        String joined = list.stream().map(Named::name).filter(Objects::nonNull)
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private static String firstName(List<Named> list) {
        if (list == null) return null;
        return list.stream().map(Named::name).filter(n -> !isBlank(n)).findFirst().orElse(null);
    }

    /** Primeiro assunto que pareça gênero (ignora marcações como "Accessible book"). */
    private static String genre(List<Named> subjects) {
        if (subjects == null) return null;
        return subjects.stream().map(Named::name)
                .filter(n -> !isBlank(n) && !NOT_A_GENRE.contains(n.trim().toLowerCase()))
                .findFirst().orElse(null);
    }

    /** "May 1, 2008", "2008", "2008-05" -> 2008. Sem ano reconhecível -> null. */
    private static Integer year(String publishDate) {
        if (publishDate == null) return null;
        Matcher m = YEAR.matcher(publishDate);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String cut(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() > max ? v.substring(0, max).trim() : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ===================================================================== formato da Open Library

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenLibraryBook(
            String title,
            List<Named> authors,
            List<Named> publishers,
            @JsonProperty("publish_date") String publishDate,
            List<Named> subjects) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Named(String name) {
    }
}
