package com.joysistvi.CyberAccess.model;
public class DrinkOrderItems {
    private int id,orderId,drinkItemId,quantity; private double unitPrice;
    public DrinkOrderItems(int id,int orderId,int drinkItemId,int quantity,double unitPrice){this.id=id;this.orderId=orderId;this.drinkItemId=drinkItemId;this.quantity=quantity;this.unitPrice=unitPrice;}
    public DrinkOrderItems(int orderId,int drinkItemId,int quantity,double unitPrice){this(0,orderId,drinkItemId,quantity,unitPrice);}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getOrderId(){return orderId;} public void setOrderId(int v){orderId=v;}
    public int getDrinkItemId(){return drinkItemId;} public void setDrinkItemId(int v){drinkItemId=v;}
    public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
    public double getUnitPrice(){return unitPrice;} public void setUnitPrice(double v){unitPrice=v;}
}
