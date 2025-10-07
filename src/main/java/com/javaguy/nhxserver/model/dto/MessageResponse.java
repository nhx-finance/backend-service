package com.javaguy.nhxserver.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
        String message,
        String status,
        Object data,
        ErrorDetails error) {

    public MessageResponse(String message) {
        this(message, "success", null, null);
    }

    public MessageResponse(String message, Object data) {
        this(message, "success", data, null);
    }

    public MessageResponse(String message, String status) {
        this(message, status, null, null);
    }

    public static MessageResponse success(String message) {
        return new MessageResponse(message, "success");
    }

    public static MessageResponse success(String message, Object data) {
        return new MessageResponse(message, "success", data, null);
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(message, "error");
    }

    public static MessageResponse error(String message, ErrorDetails error) {
        return new MessageResponse(message, "error", null, error);
    }

    public record ErrorDetails(
            String code,
            String description,
            String field
    ) {
    }
}
