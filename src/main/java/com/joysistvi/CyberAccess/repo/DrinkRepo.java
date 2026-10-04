package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.DrinkItem; import java.sql.SQLException; import java.util.List; import java.util.Optional;
public interface DrinkRepo { List<DrinkItem> findAll()throws SQLException; List<DrinkItem> findByCategory(int id)throws SQLException; Optional<DrinkItem> findById(int id)throws SQLException; void save(DrinkItem x)throws SQLException; boolean update(DrinkItem x)throws SQLException; boolean delete(int id)throws SQLException; }
