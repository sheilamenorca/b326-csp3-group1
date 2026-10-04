package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.Order; import java.sql.SQLException; import java.util.List; import java.util.Optional;
public interface OrderRepo { List<Order> findAll()throws SQLException; List<Order> findByUser(int userId)throws SQLException; Optional<Order> findById(int id)throws SQLException; void save(Order x)throws SQLException; boolean update(Order x)throws SQLException; boolean setStatus(int id,String status)throws SQLException; boolean delete(int id)throws SQLException; }
