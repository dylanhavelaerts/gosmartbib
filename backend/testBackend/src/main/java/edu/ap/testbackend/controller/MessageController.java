package edu.ap.testbackend.controller;

import edu.ap.testbackend.entities.TestMessage;
import edu.ap.testbackend.repository.TextMessageRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Test;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // allow frontend
public class MessageController {
    private TextMessageRepository textMessageRepository;

    @GetMapping("/hello")
    public String hello(){
        return "hello from springboot!";
    }
    @PostMapping("/save")
    public TestMessage saveMessage(@RequestParam TestMessage message){
        return textMessageRepository.save(message);
    }
}
