package com.joysistvi.CyberAccess.service;
import com.joysistvi.CyberAccess.model.DrinkOrderItems; import java.util.List;
public interface DrinkOrderItemsService { List<DrinkOrderItems> getAll(); List<DrinkOrderItems> getByOrderId(int id); boolean add(DrinkOrderItems x); boolean delete(int id); }
