package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.DrinkOrderItems; import java.util.List;
public interface DrinkOrderItemsRepo { List<DrinkOrderItems> getAll(); List<DrinkOrderItems> getByOrderId(int orderId); boolean add(DrinkOrderItems item); boolean delete(int id); }
