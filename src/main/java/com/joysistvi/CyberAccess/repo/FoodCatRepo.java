package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.FoodCat; import java.sql.SQLException; import java.util.List; import java.util.Optional;
public interface FoodCatRepo { List<FoodCat> findAll() throws SQLException; Optional<FoodCat> findById(int id)throws SQLException; void save(FoodCat x)throws SQLException; boolean update(FoodCat x)throws SQLException; boolean delete(int id)throws SQLException; }
