package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.config.DatabaseConnection; import com.joysistvi.CyberAccess.model.Food; import java.sql.*; import java.util.*;
public class FoodRepoImpl implements FoodRepo {
 private final DatabaseConnection db; public FoodRepoImpl(){this(new DatabaseConnection());} public FoodRepoImpl(DatabaseConnection db){this.db=db;}
 private Food map(ResultSet r)throws SQLException{return new Food(r.getInt("id"),r.getString("food"),r.getInt("category_id"),r.getBigDecimal("price"));}
 public List<Food> findAll()throws SQLException{return list("SELECT id,food,category_id,price FROM food_items ORDER BY id",null);}
 public List<Food> findByCategory(int id)throws SQLException{return list("SELECT id,food,category_id,price FROM food_items WHERE category_id=? ORDER BY id",id);}
 private List<Food> list(String q,Integer id)throws SQLException{List<Food>a=new ArrayList<>();try(Connection c=db.connect();PreparedStatement s=c.prepareStatement(q)){if(id!=null)s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())a.add(map(r));}}return a;}
 public Optional<Food> findById(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("SELECT id,food,category_id,price FROM food_items WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
 public void save(Food x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("INSERT INTO food_items(food,category_id,price) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,x.getFood());s.setInt(2,x.getCategoryId());s.setBigDecimal(3,x.getPrice());s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}}
 public boolean update(Food x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("UPDATE food_items SET food=?,category_id=?,price=? WHERE id=?")){s.setString(1,x.getFood());s.setInt(2,x.getCategoryId());s.setBigDecimal(3,x.getPrice());s.setInt(4,x.getId());return s.executeUpdate()>0;}}
 public boolean delete(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("DELETE FROM food_items WHERE id=?")){s.setInt(1,id);return s.executeUpdate()>0;}}
}
