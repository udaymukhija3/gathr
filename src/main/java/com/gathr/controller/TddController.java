package com.gathr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tdd")
public class TddController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> helloWorld() {
        return ResponseEntity.ok(Map.of("message", "Hello TDD!"));
    }
}
