package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.config.DatabaseConnection; import com.joysistvi.CyberAccess.model.DrinkItem; import java.sql.*; import java.util.*;
public class DrinkRepoImpl implements DrinkRepo {
 private final DatabaseConnection db; public DrinkRepoImpl(){this(new DatabaseConnection());} public DrinkRepoImpl(DatabaseConnection db){this.db=db;}
 private DrinkItem map(ResultSet r)throws SQLException{return new DrinkItem(r.getInt("id"),r.getString("name"),r.getInt("category_id"),r.getBigDecimal("price"));}
 public List<DrinkItem> findAll()throws SQLException{return list("SELECT id,name,category_id,price FROM drink_items ORDER BY id",null);}
 public List<DrinkItem> findByCategory(int id)throws SQLException{return list("SELECT id,name,category_id,price FROM drink_items WHERE category_id=? ORDER BY id",id);}
 private List<DrinkItem> list(String q,Integer id)throws SQLException{List<DrinkItem>a=new ArrayList<>();try(Connection c=db.connect();PreparedStatement s=c.prepareStatement(q)){if(id!=null)s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())a.add(map(r));}}return a;}
 public Optional<DrinkItem> findById(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("SELECT id,name,category_id,price FROM drink_items WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
 public void save(DrinkItem x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("INSERT INTO drink_items(name,category_id,price) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,x.getName());s.setInt(2,x.getCategoryId());s.setBigDecimal(3,x.getPrice());s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}}
 public boolean update(DrinkItem x)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("UPDATE drink_items SET name=?,category_id=?,price=? WHERE id=?")){s.setString(1,x.getName());s.setInt(2,x.getCategoryId());s.setBigDecimal(3,x.getPrice());s.setInt(4,x.getId());return s.executeUpdate()>0;}}
 public boolean delete(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("DELETE FROM drink_items WHERE id=?")){s.setInt(1,id);return s.executeUpdate()>0;}}
}
