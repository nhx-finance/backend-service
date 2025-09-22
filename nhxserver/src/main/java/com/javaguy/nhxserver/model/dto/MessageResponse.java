package com.javaguy.nhxserver.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private String message;
    private String status;
    private Object data;
    private ErrorDetails error;

    public MessageResponse(String message) {
        this.message = message;
        this.status = "success";
    }

    public MessageResponse(String message, Object data) {
        this.message = message;
        this.status = "success";
        this.data = data;
    }

    public MessageResponse(String message, String status) {
        this.message = message;
        this.status = status;
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetails {
        private String code;
        private String description;
        private String field;
    }
}
