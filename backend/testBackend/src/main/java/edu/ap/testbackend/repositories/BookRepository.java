package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.BookEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    List<BookEntity> findTop4BySpotlightTrueOrderByIdDesc();

    List<BookEntity> findTop4ByOrderByIdDesc();

}
