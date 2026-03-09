package edu.ap.testbackend.util;

import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repositories.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeding implements CommandLineRunner {
        private final BookRepository bookRepository;
        private static final Logger logger = LoggerFactory.getLogger(DataSeeding.class);

        public DataSeeding(BookRepository bookRepository) {
                this.bookRepository = bookRepository;
        }

        private BookEntity book(
                        String title,
                        List<String> authors,
                        String publisher,
                        String description,
                        int pageCount,
                        List<String> categories,
                        String thumbnail,
                        String language,
                        double rating,
                        String isbn,
                        Integer publishedYear,
                        boolean spotlight) {
                BookEntity b = new BookEntity(
                                title,
                                authors,
                                publisher,
                                description,
                                pageCount,
                                categories,
                                thumbnail,
                                language,
                                rating,
                                isbn,
                                publishedYear);
                b.setSpotlight(spotlight);
                return b;
        }

        @Override
        public void run(String... args) {
                if (bookRepository.count() == 0) {

                        BookEntity book1 = book(
                                        "Clean Code",
                                        List.of("Robert C. Martin"),
                                        "Prentice Hall",
                                        "A handbook of agile software craftsmanship focusing on writing maintainable code.",
                                        464,
                                        List.of("Programming", "Software Engineering"),
                                        "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
                                        "en",
                                        4.7,
                                        "9780132350884",
                                        2008,
                                        true);

                        BookEntity book2 = book(
                                        "The Pragmatic Programmer",
                                        List.of("Andrew Hunt", "David Thomas"),
                                        "Addison-Wesley",
                                        "Classic guide for pragmatic and effective software development practices.",
                                        352,
                                        List.of("Programming", "Best Practices"),
                                        "https://covers.openlibrary.org/b/isbn/9780201616224-L.jpg",
                                        "en",
                                        4.6,
                                        "9780201616224",
                                        1999,
                                        true);

                        BookEntity book3 = book(
                                        "Design Patterns: Elements of Reusable Object-Oriented Software",
                                        List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"),
                                        "Addison-Wesley",
                                        "The original Gang of Four book introducing foundational design patterns.",
                                        395,
                                        List.of("Programming", "Architecture"),
                                        "https://covers.openlibrary.org/b/isbn/9780201633610-L.jpg",
                                        "en",
                                        4.5,
                                        "9780201633610",
                                        1994,
                                        false);

                        BookEntity book4 = book(
                                        "Refactoring",
                                        List.of("Martin Fowler"),
                                        "Addison-Wesley",
                                        "Improving the design of existing code through structured refactoring techniques.",
                                        448,
                                        List.of("Programming", "Code Quality"),
                                        "https://covers.openlibrary.org/b/isbn/9780201485677-L.jpg",
                                        "en",
                                        4.5,
                                        "9780201485677",
                                        1999,
                                        false);

                        BookEntity book5 = book(
                                        "Effective Java",
                                        List.of("Joshua Bloch"),
                                        "Addison-Wesley",
                                        "Best practices and expert recommendations for writing robust Java code.",
                                        416,
                                        List.of("Java", "Programming"),
                                        "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg",
                                        "en",
                                        4.8,
                                        "9780134685991",
                                        2018,
                                        true);

                        BookEntity book6 = book(
                                        "Spring in Action",
                                        List.of("Craig Walls"),
                                        "Manning",
                                        "Comprehensive guide to developing applications using the Spring Framework.",
                                        520,
                                        List.of("Java", "Spring"),
                                        "https://covers.openlibrary.org/b/isbn/9781617294945-L.jpg",
                                        "en",
                                        4.4,
                                        "9781617294945",
                                        2018,
                                        false);

                        BookEntity book7 = book(
                                        "Introduction to Algorithms",
                                        List.of("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest",
                                                        "Clifford Stein"),
                                        "MIT Press",
                                        "Widely used textbook covering a broad range of algorithms in depth.",
                                        1312,
                                        List.of("Algorithms", "Computer Science"),
                                        "https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg",
                                        "en",
                                        4.6,
                                        "9780262033848",
                                        2009,
                                        false);

                        BookEntity book8 = book(
                                        "You Don't Know JS Yet",
                                        List.of("Kyle Simpson"),
                                        "Independently published",
                                        "Deep dive into JavaScript core mechanisms and advanced concepts.",
                                        143,
                                        List.of("JavaScript", "Web Development"),
                                        "https://covers.openlibrary.org/b/isbn/9781091210099-L.jpg",
                                        "en",
                                        4.4,
                                        "9781091210099",
                                        2020,
                                        false);

                        BookEntity book9 = book(
                                        "The Clean Coder",
                                        List.of("Robert C. Martin"),
                                        "Prentice Hall",
                                        "A code of conduct for professional programmers.",
                                        256,
                                        List.of("Programming", "Career"),
                                        "https://covers.openlibrary.org/b/isbn/9780137081073-L.jpg",
                                        "en",
                                        4.3,
                                        "9780137081073",
                                        2011,
                                        false);

                        BookEntity book10 = book(
                                        "Head First Design Patterns",
                                        List.of("Eric Freeman", "Elisabeth Robson"),
                                        "O'Reilly Media",
                                        "A visually rich introduction to design patterns in software development.",
                                        694,
                                        List.of("Programming", "Design Patterns"),
                                        "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg",
                                        "en",
                                        4.6,
                                        "9780596007126",
                                        2004,
                                        false);

                        BookEntity book11 = book(
                                        "Domain-Driven Design",
                                        List.of("Eric Evans"),
                                        "Addison-Wesley",
                                        "Tackling complexity in the heart of software with domain-driven design.",
                                        560,
                                        List.of("Architecture", "Software Design"),
                                        "https://covers.openlibrary.org/b/isbn/9780321125217-L.jpg",
                                        "en",
                                        4.5,
                                        "9780321125217",
                                        2003,
                                        false);

                        BookEntity book12 = book(
                                        "Clean Architecture",
                                        List.of("Robert C. Martin"),
                                        "Prentice Hall",
                                        "A guide to building scalable and maintainable software architecture.",
                                        432,
                                        List.of("Architecture", "Software Engineering"),
                                        "https://covers.openlibrary.org/b/isbn/9780134494166-L.jpg",
                                        "en",
                                        4.4,
                                        "9780134494166",
                                        2017,
                                        true);

                        bookRepository.saveAll(List.of(
                                        book1, book2, book3, book4, book5, book6,
                                        book7, book8, book9, book10, book11, book12));

                        logger.info("Database seeded with 12 books!");
                } else {
                        logger.info("Database already contains data; seeding skipped.");
                }
        }
}