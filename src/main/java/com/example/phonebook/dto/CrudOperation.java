package com.example.phonebook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrudOperation {
    private String op;
    private String by;
    private Long id;
    private String userName;
    private List<String> phoneNumbers;
    private Updates updates;
    private String explanation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Updates {
        private String userName;
        private List<String> phoneNumbers;
    }
}
