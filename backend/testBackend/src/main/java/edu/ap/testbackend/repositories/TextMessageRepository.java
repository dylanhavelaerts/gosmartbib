package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.TestMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextMessageRepository extends JpaRepository<TestMessage, Long> {

}
