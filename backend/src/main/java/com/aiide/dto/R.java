package com.aiide.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class R<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        return R.<T>builder().code(200).message("success").data(data).build();
    }

    public static <T> R<T> ok() {
        return R.<T>builder().code(200).message("success").build();
    }

    public static <T> R<T> error(String message) {
        return R.<T>builder().code(500).message(message).build();
    }

    public static <T> R<T> error(Integer code, String message) {
        return R.<T>builder().code(code).message(message).build();
    }
}
