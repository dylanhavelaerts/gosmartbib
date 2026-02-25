package edu.ap.testbackend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="tblBooks")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @ElementCollection
    private List<String> authors;
    private String publisher;
    private String description;
    private Integer pageCount;
    @ElementCollection
    private List<String> categories;
    private String thumbnail;
    private String language;
    private Double rating;


    public BookEntity(){}

    public BookEntity(String title, List<String> authors, String publisher, String description, int pageCount, List<String> categories, String thumbnail, String language, double rating) {
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.description = description;
        this.pageCount = pageCount;
        this.categories = categories;
        this.thumbnail = thumbnail;
        this.language = language;
        this.rating = rating;
    }

    @Override
    public String toString() {
        return "BookEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", authors=" + authors +
                ", publisher='" + publisher + '\'' +
                ", description='" + description + '\'' +
                ", pageCount=" + pageCount +
                ", categories=" + categories +
                ", thumbnail='" + thumbnail + '\'' +
                ", language='" + language + '\'' +
                ", rating=" + rating +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookEntity)) return false;
        BookEntity that = (BookEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
