package com.javaguy.nhxserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.entity.AuditLog;
import com.javaguy.nhxserver.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Logs an audit event for a transaction.
     *
     * @param transactionId The ID of the transaction.
     * @param eventType     The type of event (e.g., "STATUS_CHANGE", "PAYMENT_INITIATED").
     * @param oldStatus     The old status (nullable).
     * @param newStatus     The new status.
     * @param details       Additional details as a map (will be serialized to JSON).
     */
    @Transactional
    public void logTransactionEvent(UUID transactionId, String eventType, String oldStatus, String newStatus, Map<String, Object> details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.error("Error serializing audit log details to JSON: {}", e.getMessage());
                // Continue without details if serialization fails
            }
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setTransactionId(transactionId);
        auditLog.setEventType(eventType);
        auditLog.setOldStatus(oldStatus);
        auditLog.setNewStatus(newStatus);
        auditLog.setDetails(detailsJson);

        auditLogRepository.save(auditLog);
        log.debug("Audit log saved for transaction {}: Event: {}, New Status: {}", transactionId, eventType, newStatus);
    }

    /**
     * Logs a generic audit event not tied to a specific transaction.
     *
     * @param eventType The type of event.
     * @param details   Additional details as a map.
     */
    @Transactional
    public void logGenericEvent(String eventType, Map<String, Object> details) {
        logTransactionEvent(null, eventType, null, null, details);
    }
}
