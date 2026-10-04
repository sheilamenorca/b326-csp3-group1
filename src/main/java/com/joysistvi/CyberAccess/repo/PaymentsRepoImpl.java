package com.joysistvi.CyberAccess.repo;

import com.joysistvi.CyberAccess.config.DatabaseConnection;
import com.joysistvi.CyberAccess.model.Payments;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentsRepoImpl implements PaymentsRepo {
    private final DatabaseConnection db;

    public PaymentsRepoImpl() { this(new DatabaseConnection()); }
    public PaymentsRepoImpl(DatabaseConnection db) { this.db = db; }

    private Payments map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("payment_date");
        return new Payments(
                rs.getInt("id"),
                rs.getInt("transaction_id"),
                rs.getBigDecimal("amount"),
                rs.getString("payment_method"),
                ts == null ? null : ts.toLocalDateTime(),
                rs.getString("status")
        );
    }

    private static final String SELECT = "SELECT id, transaction_id, amount, payment_method, payment_date, status FROM payments";

    @Override public List<Payments> findAll() throws SQLException {
        List<Payments> list = new ArrayList<>();
        try (Connection c=db.connect(); PreparedStatement s=c.prepareStatement(SELECT+" ORDER BY id DESC"); ResultSet r=s.executeQuery()) {
            while(r.next()) list.add(map(r));
        }
        return list;
    }

    @Override public Optional<Payments> findById(int id) throws SQLException {
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement(SELECT+" WHERE id=?")) {
            s.setInt(1,id); try(ResultSet r=s.executeQuery()) { return r.next()?Optional.of(map(r)):Optional.empty(); }
        }
    }

    @Override public List<Payments> findByTransactionId(int transactionId) throws SQLException {
        return findList(SELECT+" WHERE transaction_id=? ORDER BY id DESC", transactionId);
    }

    @Override public List<Payments> findByUserId(int userId) throws SQLException {
        String sql = SELECT+" p JOIN transactions t ON t.id=p.transaction_id WHERE t.user_id=? ORDER BY p.id DESC";
        return findList(sql, userId);
    }

    @Override public List<Payments> findByStatus(String status) throws SQLException {
        return findList(SELECT+" WHERE status=? ORDER BY id DESC", status);
    }

    private List<Payments> findList(String sql, Object value) throws SQLException {
        List<Payments> list=new ArrayList<>();
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement(sql)) {
            if(value instanceof Integer i) s.setInt(1,i); else s.setString(1,String.valueOf(value));
            try(ResultSet r=s.executeQuery()) { while(r.next()) list.add(map(r)); }
        }
        return list;
    }

    @Override public boolean existsById(int id) throws SQLException {
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement("SELECT 1 FROM payments WHERE id=?")) {
            s.setInt(1,id); try(ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }

    @Override public void save(Payments p) throws SQLException {
        String sql="INSERT INTO payments(transaction_id, amount, payment_method, payment_date, status) VALUES(?,?,?,?,?)";
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1,p.getTransactionId()); s.setBigDecimal(2,p.getAmount()); s.setString(3,p.getPaymentMethod());
            if(p.getPaymentDate()==null) s.setTimestamp(4,Timestamp.valueOf(java.time.LocalDateTime.now()));
            else s.setTimestamp(4,Timestamp.valueOf(p.getPaymentDate()));
            s.setString(5,p.getStatus()); s.executeUpdate();
            try(ResultSet k=s.getGeneratedKeys()){if(k.next())p.setId(k.getInt(1));}
        }
    }

    @Override public boolean update(Payments p) throws SQLException {
        String sql="UPDATE payments SET transaction_id=?, amount=?, payment_method=?, payment_date=?, status=? WHERE id=?";
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,p.getTransactionId()); s.setBigDecimal(2,p.getAmount()); s.setString(3,p.getPaymentMethod());
            s.setTimestamp(4,Timestamp.valueOf(p.getPaymentDate())); s.setString(5,p.getStatus()); s.setInt(6,p.getId());
            return s.executeUpdate()>0;
        }
    }

    @Override public boolean updateStatus(int id,String status) throws SQLException {
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement("UPDATE payments SET status=? WHERE id=?")) {
            s.setString(1,status);s.setInt(2,id);return s.executeUpdate()>0;
        }
    }

    @Override public boolean delete(int id) throws SQLException {
        try(Connection c=db.connect(); PreparedStatement s=c.prepareStatement("DELETE FROM payments WHERE id=?")) {
            s.setInt(1,id);return s.executeUpdate()>0;
        }
    }
}
