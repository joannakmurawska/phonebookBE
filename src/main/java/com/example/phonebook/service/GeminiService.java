package com.example.phonebook.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public String generateContent(String prompt) {
        String apiKey = this.apiKey;
        String model = this.model;
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

        int maxRetries = 3;
        int retryDelay = 1000;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                RequestBody body = RequestBody.create(
                        gson.toJson(Map.of(
                                "contents", List.of(Map.of(
                                        "parts", List.of(Map.of("text", prompt))
                                ))
                        )),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 429) {

                        log.warn("Rate limited (429), attempt {}/{}. Waiting {}ms before retry...",
                                attempt + 1, maxRetries, retryDelay);
                        if (attempt < maxRetries - 1) {
                            Thread.sleep(retryDelay);
                            retryDelay *= 2;
                            continue;
                        }
                    }

                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        log.error("Gemini API error: HTTP {}, Body: {}", response.code(), errorBody);
                        throw new RuntimeException("Failed to call Gemini API: " + response.code());
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        throw new RuntimeException("Empty response from Gemini API");
                    }

                    String responseText = responseBody.string();
                    Map<String, Object> responseMap = gson.fromJson(responseText, Map.class);


                    List<?> candidates = (List<?>) responseMap.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        if (content != null) {
                            List<?> parts = (List<?>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map<String, Object> part = (Map<String, Object>) parts.get(0);
                                Object text = part.get("text");
                                return text != null ? text.toString() : "";
                            }
                        }
                    }
                    return "";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted: " + e.getMessage(), e);
            } catch (IOException e) {
                log.error("IOException calling Gemini API", e);
                throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Failed to call Gemini API after " + maxRetries + " retries");
    }


    public String parseCrudOperation(String userRequest, String currentUsers) {
        String prompt = String.format(
                "You are an AI assistant that understands CRUD operations for a phonebook application.\n" +
                        "Current users in the database:\n%s\n\n" +
                        "User request: \"%s\"\n\n" +
                        "Analyze the request and respond with ONLY a JSON object in this exact format:\n" +
                        "{\n" +
                        "  \"op\": \"create\" or \"read\" or \"update\" or \"delete\",\n" +
                        "  \"by\": \"id\" or \"name\" or null,\n" +
                        "  \"id\": null or user ID number,\n" +
                        "  \"userName\": \"name\" or null,\n" +
                        "  \"phoneNumbers\": [],\n" +
                        "  \"updates\": { \"userName\": null, \"phoneNumbers\": [] } or null,\n" +
                        "  \"explanation\": \"brief explanation\"\n" +
                        "}\n\n" +
                        "IMPORTANT:\n" +
                        "- For UPDATE operations: Always put changes in 'updates' field\n" +
                        "- For CREATE: phoneNumbers go directly in root, updates is null\n" +
                        "- Never mix phoneNumbers in root AND updates at the same time for UPDATE\n" +
                        "Examples:\n" +
                        "- \"Add phone to Joanna\" -> {\"op\":\"update\",\"by\":\"name\",\"userName\":\"Joanna\",\"phoneNumbers\":[],\"updates\":{\"phoneNumbers\":[\"+48123456789\"]},\"explanation\":\"...\"}\n" +
                        "- \"Add John with 500123456\" -> {\"op\":\"create\",\"by\":null,\"id\":null,\"userName\":\"John\",\"phoneNumbers\":[\"+48500123456\"],\"updates\":null,\"explanation\":\"...\"}\n\n" +
                        "Respond with ONLY the JSON object, no additional text.",
                currentUsers, userRequest
        );

        return generateContent(prompt);
    }
}
