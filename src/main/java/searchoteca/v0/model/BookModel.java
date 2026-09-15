package searchoteca.v0.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name= "org_books")
public class BookModel {
    @Id
    @Column("book_id")
    private Long Id;

    private String isbn;
    private String title;
    private String author;
    private int rel_year;
    private String publisher;
    private String genre;

    @Column("depart_code")
    private String departCode;

    @Column("local_code")
    private String localCode;

    public BookModel() {}
}
