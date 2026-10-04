package com.joysistvi.CyberAccess.service;

import com.joysistvi.CyberAccess.model.Conversation;
import com.joysistvi.CyberAccess.repo.ConversationRepo;
import com.joysistvi.CyberAccess.repo.ConversationRepoImpl;

import java.sql.SQLException;
import java.util.List;

public class ConversationServiceImpl {
    private final ConversationRepo repo;

    public ConversationServiceImpl() { this(new ConversationRepoImpl()); }
    public ConversationServiceImpl(ConversationRepo repo) { this.repo = repo; }

    public List<Conversation> getAll() throws SQLException {
        return repo.findAll();
    }

    public List<Conversation> getByUser(int id) throws SQLException {
        return repo.findByUser(Validation.id(id));
    }

    public boolean send(Conversation conversation) throws SQLException {
        if (conversation == null) {
            throw new IllegalArgumentException("Message is required.");
        }

        Validation.id(conversation.getUserId());
        conversation.setMessage(Validation.text(conversation.getMessage(), "Message", 500));
        conversation.setSenderType(Validation.text(conversation.getSenderType(), "Sender type", 50));

        if (conversation.getCreatedAt() == null) {
            conversation.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (conversation.getStatus() == null || conversation.getStatus().isBlank()) {
            conversation.setStatus("Active");
        }

        return repo.save(conversation);
    }

    public boolean setStatus(int id, String status) throws SQLException {
        return repo.updateStatus(Validation.id(id), Validation.text(status, "Status", 50));
    }

    public boolean delete(int id) throws SQLException {
        return repo.delete(Validation.id(id));
    }
}
