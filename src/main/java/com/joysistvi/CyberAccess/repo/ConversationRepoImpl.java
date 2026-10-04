package com.joysistvi.CyberAccess.repo;

import com.joysistvi.CyberAccess.config.DatabaseConnection;
import com.joysistvi.CyberAccess.model.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationRepoImpl implements ConversationRepo {
    private final DatabaseConnection db;

    public ConversationRepoImpl() { this(new DatabaseConnection()); }
    public ConversationRepoImpl(DatabaseConnection db) { this.db = db; }

    private Conversation map(ResultSet r) throws SQLException {
        Timestamp t = r.getTimestamp("created_at");
        return new Conversation(
                r.getInt("id"),
                r.getInt("user_id"),
                r.getString("message"),
                r.getString("sender_type"),
                t == null ? null : t.toLocalDateTime(),
                r.getString("status")
        );
    }

    private List<Conversation> list(String query, Integer userId) throws SQLException {
        List<Conversation> rows = new ArrayList<>();
        try (Connection c = db.connect(); PreparedStatement s = c.prepareStatement(query)) {
            if (userId != null) s.setInt(1, userId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(map(r));
            }
        }
        return rows;
    }

    @Override
    public List<Conversation> findAll() throws SQLException {
        return list("SELECT id,user_id,message,sender_type,created_at,status " +
                "FROM conversations ORDER BY id DESC", null);
    }

    @Override
    public List<Conversation> findByUser(int id) throws SQLException {
        return list("SELECT id,user_id,message,sender_type,created_at,status " +
                "FROM conversations WHERE user_id=? ORDER BY id", id);
    }

    @Override
    public boolean save(Conversation x) throws SQLException {
        String query = "INSERT INTO conversations(user_id,message,sender_type,created_at,status) " +
                "VALUES(?,?,?,?,?)";

        try (Connection c = db.connect();
             PreparedStatement s = c.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, x.getUserId());
            s.setString(2, x.getMessage());
            s.setString(3, x.getSenderType());
            s.setTimestamp(4, Timestamp.valueOf(
                    x.getCreatedAt() == null ? java.time.LocalDateTime.now() : x.getCreatedAt()));
            s.setString(5, x.getStatus());

            if (s.executeUpdate() == 0) return false;

            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) x.setId(keys.getInt(1));
            }
            return true;
        }
    }

    @Override
    public boolean updateStatus(int id, String status) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE conversations SET status=? WHERE id=?")) {
            s.setString(1, status);
            s.setInt(2, id);
            return s.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement s = c.prepareStatement(
                     "DELETE FROM conversations WHERE id=?")) {
            s.setInt(1, id);
            return s.executeUpdate() > 0;
        }
    }
}
