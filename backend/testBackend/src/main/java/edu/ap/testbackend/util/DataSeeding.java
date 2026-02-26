package edu.ap.testbackend.util;

import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeding implements CommandLineRunner {
    private final BookRepository bookRepository;

    public DataSeeding(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            BookEntity book1 = new BookEntity(
                    "The Pragmatic Programmer",
                    List.of("David Thomas", "Andrew Hunt"),
                    "Addison-Wesley",
                    "A guide to pragmatic programming practices.",
                    352, List.of("Programming", "Software Engineering"),
                    "https://example.com/pragmatic.jpg",
                    "en", 4.5
            );

            BookEntity book2 = new BookEntity(
                    "Clean Code",
                    List.of("Robert C. Martin"),
                    "Prentice Hall",
                    "A handbook of agile software craftsmanship.",
                    431, List.of("Programming", "Best Practices"),
                    "https://example.com/cleancode.jpg",
                    "en", 4.3
            );

            BookEntity book3 = new BookEntity(
                    "Spring Boot in Action",
                    List.of("Craig Walls"),
                    "Manning",
                    "A practical guide to Spring Boot.",
                    264, List.of("Java", "Spring"),
                    "https://example.com/springboot.jpg",
                    "en", 4.1
            );

            bookRepository.saveAll(List.of(book1, book2, book3));
            System.out.println("Database seeded!");
        }
    }
}
