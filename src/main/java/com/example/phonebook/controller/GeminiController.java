package com.example.phonebook.controller;

import com.example.phonebook.dto.GeminiCrudRequest;
import com.example.phonebook.service.GeminiService;
import com.example.phonebook.service.IntelligentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;
    private final IntelligentUserService intelligentUserService;

    @PostMapping("/prompt")
    public ResponseEntity<Map<String, String>> sendPrompt(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String response = geminiService.generateContent(prompt);
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping("/user-crud")
    public ResponseEntity<Map<String, Object>> processUserCrud(@RequestBody GeminiCrudRequest request) {
        Map<String, Object> response = intelligentUserService.processNaturalLanguageRequest(request.getUserRequest());
        return ResponseEntity.ok(response);
    }
}
