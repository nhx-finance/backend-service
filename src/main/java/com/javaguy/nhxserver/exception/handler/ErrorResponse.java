package com.javaguy.nhxserver.exception.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Contract-compliant error envelope: { "error": { code, message, details } }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private ErrorDetail error;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private Map<String, Object> details;
    }
}
