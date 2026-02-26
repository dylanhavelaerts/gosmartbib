package edu.ap.testbackend.controllers;

import edu.ap.testbackend.entities.TestMessage;
import edu.ap.testbackend.repositories.TextMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageController {
    private final TextMessageRepository textMessageRepository;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        List<TestMessage> messages = textMessageRepository.findAll();
        if (messages.isEmpty()) {
            return Map.of("message", "No messages in database yet.");
        }
        return Map.of("message", messages.get(0).getText());
    }

    @GetMapping("/messages")
    public List<TestMessage> getMessages() {
        return textMessageRepository.findAll();
    }

    @PostMapping("/save")
    public TestMessage saveMessage(@RequestBody TestMessage message) {
        return textMessageRepository.save(message);
    }
}