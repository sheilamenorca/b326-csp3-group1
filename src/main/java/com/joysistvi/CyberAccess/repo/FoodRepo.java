package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.Food; import java.sql.SQLException; import java.util.List; import java.util.Optional;
public interface FoodRepo { List<Food> findAll()throws SQLException; List<Food> findByCategory(int categoryId)throws SQLException; Optional<Food> findById(int id)throws SQLException; void save(Food x)throws SQLException; boolean update(Food x)throws SQLException; boolean delete(int id)throws SQLException; }
