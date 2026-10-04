package com.joysistvi.CyberAccess.repo;

import com.joysistvi.CyberAccess.model.ServiceRate;
import com.joysistvi.CyberAccess.config.DatabaseConnection;
import com.joysistvi.CyberAccess.model.Transactions;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Runs SQL for transactions, charges, rates and history.
public class TransactionsRepoImpl implements TransactionsRepo{
    private final DatabaseConnection db;
    public TransactionsRepoImpl(DatabaseConnection db) { this.db = db; }

    // Create an Open transaction only if the user exists and is active.
    public int create(int userId) throws SQLException {
        // The supplied database schema does not mark transactions.id as AUTO_INCREMENT,
        // so generate the next ID explicitly instead of relying on MySQL defaults.
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                int transactionId;
                try (PreparedStatement idStmt = c.prepareStatement(
                        "SELECT COALESCE(MAX(id), 0) + 1 FROM transactions")) {
                    try (ResultSet r = idStmt.executeQuery()) {
                        r.next();
                        transactionId = r.getInt(1);
                    }
                }
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO transactions(id, user_id, status) SELECT ?, id, 'Open' FROM users WHERE id=? AND status='active'")) {
                    s.setInt(1, transactionId);
                    s.setInt(2, userId);
                    if (s.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Active user not found. Use an existing users.id.");
                    }
                }
                c.commit();
                return transactionId;
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        }
    }
    // A null user ID means show everyone; otherwise show only that user.
    public List<Transactions> history(Integer userId) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.created_at, t.status, "
                + "COALESCE(SUM(ROUND(i.quantity*i.unit_price, 2)), 0.00) AS total "
                + "FROM transactions t LEFT JOIN transaction_items i ON i.transaction_id=t.id "
                + (userId == null ? "" : "WHERE t.user_id=? ")
                + "GROUP BY t.id, t.user_id, t.created_at, t.status ORDER BY t.created_at DESC, t.id DESC";
        List<Transactions> rows = new ArrayList<>();
        try (Connection c = db.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            if (userId != null) s.setInt(1, userId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new Transactions(r.getInt("id"), r.getInt("user_id"),
                        r.getTimestamp("created_at").toLocalDateTime(), r.getString("status"), r.getBigDecimal("total")));
            }
        }
        return rows;
    }
    // Lock the parent before every charge/status change so closing a bill cannot race an edit.
    private void lockOpen(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT status FROM transactions WHERE id=? FOR UPDATE")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Transaction not found.");
                if (!"Open".equals(r.getString(1))) throw new IllegalArgumentException("Only Open transactions can be changed.");
            }
        }
    }
    // Save a custom charge or a charge linked to a service.
    public int addItem(int transactionId, Integer serviceId, String description,
                       BigDecimal quantity, String unit, BigDecimal price) throws SQLException {
        try (Connection c = db.connect()) {
            // Save the following steps together only when commit() succeeds.
            c.setAutoCommit(false);
            try {
                lockOpen(c, transactionId);
                // Copy the current name into the bill: later renaming does not change old bills.
                if (serviceId != null) {
                    try (PreparedStatement s = c.prepareStatement("SELECT name, status FROM services WHERE id=? FOR UPDATE")) {
                        s.setInt(1, serviceId);
                        try (ResultSet r = s.executeQuery()) {
                            if (!r.next()) throw new IllegalArgumentException("Service not found.");
                            if (!"Active".equals(r.getString("status"))) throw new IllegalArgumentException("Service is inactive.");
                            description = r.getString("name");
                        }
                    }
                }
                int id = nextTransactionItemId(c);
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO transaction_items(id, transaction_id, service_id, description, quantity, unit, unit_price) VALUES (?,?,?,?,?,?,?)")) {
                    s.setInt(1, id);
                    s.setInt(2, transactionId);
                    if (serviceId == null) s.setNull(3, Types.INTEGER); else s.setInt(3, serviceId);
                    s.setString(4, description); s.setBigDecimal(5, quantity); s.setString(6, unit); s.setBigDecimal(7, price);
                    s.executeUpdate();
                }
                c.commit(); return id;
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }
    // Show only active rates connected to active services.
    public List<ServiceRate> activeRates() throws SQLException {
        var rows = new ArrayList<ServiceRate>();
        try (Connection c = db.connect(); PreparedStatement s = c.prepareStatement(
                "SELECT r.id, r.service_id, s.name, r.rate_name, r.price, r.unit FROM rates r "
                        + "JOIN services s ON s.id=r.service_id WHERE r.status='Active' AND s.status='Active' ORDER BY r.id");
             ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new ServiceRate(r.getInt("id"), r.getInt("service_id"),
                    r.getString("name"), r.getString("rate_name"), r.getBigDecimal("price"), r.getString("unit")));
        }
        return rows;
    }
    // Copy the saved rate into a new charge so later price changes do not alter this bill.
    public int addRate(int transactionId, int rateId, BigDecimal quantity) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                lockOpen(c, transactionId);
                int serviceId;
                String description, unit;
                BigDecimal price;
                try (PreparedStatement s = c.prepareStatement(
                        "SELECT r.service_id, r.rate_name, r.price, r.unit, r.status AS rate_status, s.status AS service_status "
                                + "FROM rates r JOIN services s ON s.id=r.service_id WHERE r.id=? FOR UPDATE")) {
                    s.setInt(1, rateId);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Rate not found or not linked to a service.");
                        if (!"Active".equals(r.getString("rate_status")) || !"Active".equals(r.getString("service_status")))
                            throw new IllegalArgumentException("Rate and service must both be Active.");
                        serviceId = r.getInt("service_id"); description = r.getString("rate_name");
                        price = r.getBigDecimal("price"); unit = r.getString("unit");
                        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Rate price cannot be negative.");
                    }
                }
                int id = nextTransactionItemId(c);
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO transaction_items(id, transaction_id, service_id, description, quantity, unit, unit_price) VALUES (?,?,?,?,?,?,?)")) {
                    s.setInt(1, id); s.setInt(2, transactionId); s.setInt(3, serviceId); s.setString(4, description);
                    s.setBigDecimal(5, quantity); s.setString(6, unit); s.setBigDecimal(7, price);
                    s.executeUpdate();
                }
                c.commit(); return id;
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }
    // Delete a charge only when it belongs to this Open transaction.
    public void removeItem(int transactionId, int itemId) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                lockOpen(c, transactionId);
                try (PreparedStatement s = c.prepareStatement("DELETE FROM transaction_items WHERE id=? AND transaction_id=?")) {
                    s.setInt(1, itemId); s.setInt(2, transactionId);
                    if (s.executeUpdate() != 1) throw new IllegalArgumentException("Item does not belong to this transaction.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }
    // Close or cancel an Open transaction. Closing does not record payment.
    public void finish(int transactionId, String status) throws SQLException {
        if (!"Closed".equals(status) && !"Cancelled".equals(status))
            throw new IllegalArgumentException("Final status must be Closed or Cancelled.");
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                lockOpen(c, transactionId);
                if ("Closed".equals(status)) {
                    try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM transaction_items WHERE transaction_id=?")) {
                        s.setInt(1, transactionId);
                        try (ResultSet r = s.executeQuery()) {
                            r.next(); if (r.getInt(1) == 0) throw new IllegalArgumentException("Add a charge before closing the transaction.");
                        }
                    }
                }
                try (PreparedStatement s = c.prepareStatement("UPDATE transactions SET status=? WHERE id=?")) {
                    s.setString(1, status); s.setInt(2, transactionId); s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }

    public void markPaid(int transactionId) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                String status; BigDecimal total;
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT t.status, COALESCE(SUM(i.quantity*i.unit_price),0.00) total FROM transactions t LEFT JOIN transaction_items i ON i.transaction_id=t.id WHERE t.id=? GROUP BY t.id, t.status FOR UPDATE")) {
                    q.setInt(1, transactionId);
                    try (ResultSet r=q.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Transaction not found.");
                        status=r.getString("status"); total=r.getBigDecimal("total");
                    }
                }
                if ("Paid".equalsIgnoreCase(status)) throw new IllegalArgumentException("This transaction is already paid.");
                if ("Cancelled".equalsIgnoreCase(status)) throw new IllegalArgumentException("Cancelled transactions cannot be paid.");
                if (total == null || total.signum() <= 0) throw new IllegalArgumentException("This bill has no amount due.");
                try (PreparedStatement u=c.prepareStatement("UPDATE transactions SET status='Paid' WHERE id=?")) {
                    u.setInt(1, transactionId); u.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c,e); throw e; }
        }
    }

    private static int nextTransactionItemId(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM transaction_items")) {
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    // Kept for compatibility with older repository methods that use generated keys.
    private static int generatedId(PreparedStatement s) throws SQLException {
        try (ResultSet r = s.getGeneratedKeys()) {
            if (!r.next()) throw new SQLException("No generated ID returned.");
            return r.getInt(1);
        }
    }
    // Undo this operation if something fails, avoiding a partially saved change.
    private static void rollback(Connection c, Exception cause) {
        try { c.rollback(); } catch (SQLException e) { cause.addSuppressed(e); }
    }
}
