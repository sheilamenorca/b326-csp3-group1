package com.joysistvi.CyberAccess.model;
import java.math.BigDecimal;
public class Order {
    private int orderId; private Integer transactionId; private int userId; private BigDecimal amount; private String status;
    public Order(){}
    public Order(int orderId,Integer transactionId,int userId,BigDecimal amount,String status){this.orderId=orderId;this.transactionId=transactionId;this.userId=userId;this.amount=amount;this.status=status;}
    public Order(int userId){this(0,null,userId,BigDecimal.ZERO,"Pending");}
    public int getOrderId(){return orderId;} public void setOrderId(int v){orderId=v;}
    public Integer getTransactionId(){return transactionId;} public void setTransactionId(Integer v){transactionId=v;}
    public int getUserId(){return userId;} public void setUserId(int v){userId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
