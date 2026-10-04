package com.joysistvi.CyberAccess.model;

import java.math.BigDecimal;

public class Food {
    private int id; private String food; private int categoryId; private BigDecimal price;
    public Food() {}
    public Food(int id, String food, int categoryId, BigDecimal price){this.id=id;this.food=food;this.categoryId=categoryId;this.price=price;}
    public Food(String food,int categoryId,BigDecimal price){this(0,food,categoryId,price);}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getFood(){return food;} public void setFood(String food){this.food=food;}
    public int getCategoryId(){return categoryId;} public void setCategoryId(int categoryId){this.categoryId=categoryId;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal price){this.price=price;}
}
