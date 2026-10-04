package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.config.DatabaseConnection; import com.joysistvi.CyberAccess.model.FoodCat; import java.sql.*; import java.util.*;
public class FoodCatRepoImpl implements FoodCatRepo {
 private final DatabaseConnection db; public FoodCatRepoImpl(){this(new DatabaseConnection());} public FoodCatRepoImpl(DatabaseConnection db){this.db=db;}
 private FoodCat map(ResultSet r)throws SQLException{return new FoodCat(r.getInt("id"),r.getString("name"));}
 public List<FoodCat> findAll()throws SQLException{List<FoodCat> a=new ArrayList<>();try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("SELECT id,name FROM food_category ORDER BY id");ResultSet r=s.executeQuery()){while(r.next())a.add(map(r));}return a;}
 public Optional<FoodCat> findById(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("SELECT id,name FROM food_category WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
 public void save(FoodCat x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("INSERT INTO food_category(name) VALUES(?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,x.getName());s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}}
 public boolean update(FoodCat x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("UPDATE food_category SET name=? WHERE id=?")){s.setString(1,x.getName());s.setInt(2,x.getId());return s.executeUpdate()>0;}}
 public boolean delete(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("DELETE FROM food_category WHERE id=?")){s.setInt(1,id);return s.executeUpdate()>0;}}
}
