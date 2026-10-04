package com.joysistvi.CyberAccess.model;

import java.math.BigDecimal;

public class DrinkItem {
    private int id; private String name; private int categoryId; private BigDecimal price;
    public DrinkItem(){}
    public DrinkItem(int id,String name,int categoryId,BigDecimal price){this.id=id;this.name=name;this.categoryId=categoryId;this.price=price;}
    public DrinkItem(String name,int categoryId,BigDecimal price){this(0,name,categoryId,price);}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public int getCategoryId(){return categoryId;} public void setCategoryId(int categoryId){this.categoryId=categoryId;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal price){this.price=price;}
}
