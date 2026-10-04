package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.config.DatabaseConnection; import com.joysistvi.CyberAccess.model.DrinkOrderItems; import java.sql.*; import java.util.*;
public class DrinkOrderItemsRepoImpl implements DrinkOrderItemsRepo {
 private final DatabaseConnection db; public DrinkOrderItemsRepoImpl(){this(new DatabaseConnection());} public DrinkOrderItemsRepoImpl(DatabaseConnection db){this.db=db;}
 private DrinkOrderItems map(ResultSet r)throws SQLException{return new DrinkOrderItems(r.getInt("id"),r.getInt("order_id"),r.getInt("drink_item_id"),r.getInt("quantity"),r.getDouble("unit_price"));}
 public List<DrinkOrderItems> getAll(){return list("SELECT id,order_id,drink_item_id,quantity,unit_price FROM drink_order_items ORDER BY id DESC",null);}
 public List<DrinkOrderItems> getByOrderId(int id){return list("SELECT id,order_id,drink_item_id,quantity,unit_price FROM drink_order_items WHERE order_id=? ORDER BY id",id);}
 private List<DrinkOrderItems> list(String q,Integer id){List<DrinkOrderItems>a=new ArrayList<>();try(Connection c=db.connect();PreparedStatement s=c.prepareStatement(q)){if(id!=null)s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())a.add(map(r));}}catch(SQLException e){System.out.println("Database error: "+e.getMessage());}return a;}
 public boolean add(DrinkOrderItems x){try(Connection c=db.connect();PreparedStatement s=c.prepareStatement("INSERT INTO drink_order_items(order_id,drink_item_id,quantity,unit_price) VALUES(?,?,?,?)")){s.setInt(1,x.getOrderId());s.setInt(2,x.getDrinkItemId());s.setInt(3,x.getQuantity());s.setDouble(4,x.getUnitPrice());boolean ok=s.executeUpdate()>0;if(ok)refreshOrderAmount(c,x.getOrderId());return ok;}catch(SQLException e){System.out.println("Database error: "+e.getMessage());return false;}}
 public boolean delete(int id){
  try(Connection c=db.connect();PreparedStatement find=c.prepareStatement("SELECT order_id FROM drink_order_items WHERE id=?")){
   find.setInt(1,id); Integer orderId=null; try(ResultSet r=find.executeQuery()){if(r.next())orderId=r.getInt(1);}
   if(orderId==null)return false;
   try(PreparedStatement s=c.prepareStatement("DELETE FROM drink_order_items WHERE id=?")){s.setInt(1,id);boolean ok=s.executeUpdate()>0;if(ok)refreshOrderAmount(c,orderId);return ok;}
  }catch(SQLException e){System.out.println("Database error: "+e.getMessage());return false;}
 }
 private void refreshOrderAmount(Connection c,int orderId)throws SQLException{
  String q="UPDATE orders SET amount=COALESCE((SELECT SUM(quantity*unit_price) FROM food_order_items WHERE order_id=?),0)+COALESCE((SELECT SUM(quantity*unit_price) FROM drink_order_items WHERE order_id=?),0) WHERE order_id=?";
  try(PreparedStatement s=c.prepareStatement(q)){s.setInt(1,orderId);s.setInt(2,orderId);s.setInt(3,orderId);s.executeUpdate();}
 }
}
