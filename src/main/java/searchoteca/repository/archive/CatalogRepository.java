package searchoteca.repository.archive;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.DTO.archive.publicSearch.BookSearchResult;
import searchoteca.DTO.archive.publicSearch.CopyAvaliability;

import java.util.List;
import java.util.Map;

@Repository
public class CatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public CatalogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String WHERE = """
            WHERE b.title ILIKE :pattern
            OR b.author ILIKE :pattern
            OR b.genre ILIKE :pattern
            OR b.publisher ILIKE :pattern
            """;

    public List<BookSearchResult> search(String pattern, int limit, int offset) {
        String sql = """
                SELECT b.isbn, b.title, b.author, b.publisher, b.genre, b.relYear,
                    COUNT(c.copy_id) AS total_copies
                    COUNT(c.copy_id) FILTER (WHERE c.status = 'AVAILABLE') AS available_copies'
                FROM org_books b
                LEFT JOIN org_copies c ON c.isbn = b.isbn
                """ + WHERE + """
                GROUP BY b.book_id
                ORDER BY b.title
                LIMIT :limit OFFSET :offset
                """;
        Map<String, Object> params = Map.of("pattern", pattern, "limit", limit, "offset", offset);
        return jdbc.query(sql,params,(rs, i) -> new BookSearchResult(
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("publisher"),
                rs.getString("genre"),rs.
                getObject("relYear", Integer.class),
                rs.getInt("total_copies"),
                rs.getInt("available_copies")));
    }

    public long count(String pattern) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM org_books b "+ WHERE,
                Map.of("pattern",pattern), Long.class);
        return total == null ? 0 : total;
    }

    public List<CopyAvaliability>findAvailability(String isbn){
        String sql = """
                SELECT d.depart_code, d.depart_name, l.local_code, l.local_name,
                    COUNT(*) FILTER(WHERE c.status = 'AVAILABLE') AS available,
                    COUNT(*) AS total
                FROM org_copies c
                JOIN org_local l ON l.local_code = c.local_code
                JOIN org_depart d ON d.depart_code = c.depart_code
                WHERE c.isbn = :isbn
                GROUP BY d.depart_code, d.depart_name, l.local_code, l.local_name
                ORDER BY d.depart_code, d.depart_name, l.local_code
                """;

        return jdbc.query(sql,Map.of("isbn",isbn), (rs, i) -> new CopyAvaliability(
                rs.getString("depart_code"),rs.getString("depart_name"),
                rs.getString("local_code"),rs.getString("local_name"),
                rs.getInt("available"),rs.getInt("total")));
    }
}
