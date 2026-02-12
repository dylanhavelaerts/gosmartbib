package edu.ap.testbackend.repository;

import edu.ap.testbackend.entities.TestMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextMessageRepository extends JpaRepository<TestMessage,Long> {

}
