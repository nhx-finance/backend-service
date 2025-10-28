package com.javaguy.nhxserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import com.javaguy.nhxserver.repository.AuditLogRepository;
import com.javaguy.nhxserver.model.entity.AuditLog;

@Service
@Transactional
public class TransactionLoggingService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logTransaction(UUID transactionId, String eventType, String oldStatus, String newStatus,
            String details) {
        AuditLog log = new AuditLog();
        log.setTransactionId(transactionId);
        log.setEventType(eventType);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}