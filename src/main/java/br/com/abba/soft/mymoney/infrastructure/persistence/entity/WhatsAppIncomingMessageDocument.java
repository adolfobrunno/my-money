package br.com.abba.soft.mymoney.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "whatsapp_incoming_messages")
public class WhatsAppIncomingMessageDocument {
    @Id
    private String id;

    // WhatsApp sender phone, digits only (e.g., 5511999999999)
    @Indexed
    private String from;

    private String body;

    private LocalDateTime receivedAt;

    // Message processing status
    @Indexed
    private WhatsAppMessageStatus status;

    private String errorMessage;

    private int attempts;

    private LocalDateTime lastAttemptAt;

    public WhatsAppIncomingMessageDocument() {}

    public WhatsAppIncomingMessageDocument(String id, String from, String body, LocalDateTime receivedAt, WhatsAppMessageStatus status) {
        this.id = id;
        this.from = from;
        this.body = body;
        this.receivedAt = receivedAt;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public WhatsAppMessageStatus getStatus() { return status; }
    public void setStatus(WhatsAppMessageStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
}
