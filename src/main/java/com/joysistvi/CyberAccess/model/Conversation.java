package com.joysistvi.CyberAccess.model;

import java.time.LocalDateTime;

public class Conversation {
    private int id;
    private int userId;
    private String message;
    private String senderType;
    private String status;
    private LocalDateTime createdAt;

    public Conversation() {}

    public Conversation(int id, int userId, String message, String senderType,
                        LocalDateTime createdAt, String status) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.senderType = senderType;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Conversation(int userId, String message, String senderType) {
        this(0, userId, message, senderType, LocalDateTime.now(), "Active");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
