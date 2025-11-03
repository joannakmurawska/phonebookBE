package com.example.phonebook.service;

import com.example.phonebook.dto.CrudOperation;
import com.example.phonebook.entity.PhoneNumber;
import com.example.phonebook.entity.User;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentUserService {

    private final GeminiService geminiService;
    private final UserService userService;
    private final Gson gson = new Gson();

    @Transactional
    public Map<String, Object> processNaturalLanguageRequest(String userRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<User> currentUsers = userService.getAllUsers();
            String usersContext = formatUsersForGemini(currentUsers);

            String geminiResponse = geminiService.parseCrudOperation(userRequest, usersContext);
            log.info("Gemini response: {}", geminiResponse);

            String cleanedResponse = geminiResponse
                    .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();

            CrudOperation operation = gson.fromJson(cleanedResponse, CrudOperation.class);
            log.info("Parsed operation: {}", operation);

            Object result = executeCrudOperation(operation);

            response.put("success", true);
            response.put("operation", operation.getOp());
            response.put("explanation", operation.getExplanation());
            response.put("result", result);

        } catch (Exception e) {
            log.error("Error processing natural language request", e);
            response.put("success", false);
            response.put("error", "Failed to process request: " + e.getMessage());
        }

        return response;
    }

    private String formatUsersForGemini(List<User> users) {
        if (users.isEmpty()) {
            return "No users in database";
        }

        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            sb.append(String.format("ID: %d, Name: %s\n", user.getId(), user.getUserName()));
        }
        return sb.toString();
    }

    private Object executeCrudOperation(CrudOperation operation) {
        switch (operation.getOp().toLowerCase()) {
            case "create":
                String userName = operation.getUserName();
                List<User> existingUsers = userService.getAllUsers()
                        .stream()
                        .filter(u -> u.getUserName().equalsIgnoreCase(userName))
                        .toList();

                if (!existingUsers.isEmpty() && operation.getPhoneNumbers() != null && !operation.getPhoneNumbers().isEmpty()) {
                    User existing = existingUsers.get(0);

                    for (String phoneNumber : operation.getPhoneNumbers()) {
                        PhoneNumber phone = new PhoneNumber();
                        phone.setPhoneNumber(phoneNumber);
                        phone.setUser(existing);
                        existing.getPhoneNumbers().add(phone);
                    }

                    return userService.updateUser(existing.getId(), existing);
                } else if (!existingUsers.isEmpty()) {
                    return existingUsers.get(0);
                } else {
                    User newUser = new User();
                    newUser.setUserName(userName);

                    if (operation.getPhoneNumbers() != null && !operation.getPhoneNumbers().isEmpty()) {
                        for (String phoneNumber : operation.getPhoneNumbers()) {
                            PhoneNumber phone = new PhoneNumber();
                            phone.setPhoneNumber(phoneNumber);
                            phone.setUser(newUser);
                            newUser.getPhoneNumbers().add(phone);
                        }
                    }

                    return userService.createUser(newUser);
                }


            case "read":
                log.info("READ operation - id: {}, userName: {}", operation.getId(), operation.getUserName());

                if (operation.getId() != null) {
                    log.info("Filtering by ID: {}", operation.getId());
                    return userService.getUserById(operation.getId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                }

                String filterUserName = operation.getUserName();
                log.info("filterUserName value: '{}'", filterUserName);
                log.info("Is null: {}, Is empty: {}", filterUserName == null, filterUserName != null && filterUserName.isEmpty());

                if (filterUserName != null && !filterUserName.isEmpty()) {
                    log.info("Filtering by userName: {}", filterUserName);
                    List<User> matches = userService.getAllUsers()
                            .stream()
                            .filter(u -> {
                                boolean matches_inner = u.getUserName().equalsIgnoreCase(filterUserName);
                                log.info("  Comparing {} with {} = {}", u.getUserName(), filterUserName, matches_inner);
                                return matches_inner;
                            })
                            .toList();
                    log.info("Found {} matches", matches.size());
                    return matches;
                } else {
                    log.info("No filter - returning all users");
                    return userService.getAllUsers();
                }


            case "update":
                Long updateId = operation.getId();

                if (updateId == null && "name".equals(operation.getBy()) && operation.getUserName() != null) {
                    List<User> matches = userService.getAllUsers()
                            .stream()
                            .filter(u -> u.getUserName().equalsIgnoreCase(operation.getUserName()))
                            .toList();
                    if (!matches.isEmpty()) {
                        updateId = matches.get(0).getId();
                    }
                }

                if (updateId != null) {
                    User existingUser = userService.getUserById(updateId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    if (operation.getUpdates() != null && operation.getUpdates().getUserName() != null) {
                        existingUser.setUserName(operation.getUpdates().getUserName());
                    }

                    if (operation.getUpdates() != null && operation.getUpdates().getPhoneNumbers() != null
                            && !operation.getUpdates().getPhoneNumbers().isEmpty()) {

                        existingUser.getPhoneNumbers().clear();

                        for (String phoneNumber : operation.getUpdates().getPhoneNumbers()) {
                            PhoneNumber phone = new PhoneNumber();
                            phone.setPhoneNumber(phoneNumber);
                            phone.setUser(existingUser);
                            existingUser.getPhoneNumbers().add(phone);
                        }
                    }

                    return userService.updateUser(updateId, existingUser);
                } else {
                    return Map.of("message", "Could not resolve user to update");
                }



            case "delete":
                Long id = operation.getId();

                if (id == null && "name".equals(operation.getBy()) && operation.getUserName() != null) {
                    List<User> matches = userService.getAllUsers()
                            .stream()
                            .filter(u -> u.getUserName().equalsIgnoreCase(operation.getUserName()))
                            .toList();
                    if (!matches.isEmpty()) {
                        id = matches.get(0).getId();
                    }
                }

                if (id != null) {
                    userService.deleteUser(id);
                    return Map.of("message", "User deleted successfully");
                } else {
                    return Map.of("message", "Could not resolve user to delete");
                }

            default:
                throw new RuntimeException("Unknown operation: " + operation.getOp());
        }
    }


}
