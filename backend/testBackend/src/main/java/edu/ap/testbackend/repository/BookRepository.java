package edu.ap.testbackend.repository;

import edu.ap.testbackend.entities.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, Long > {
    
}
