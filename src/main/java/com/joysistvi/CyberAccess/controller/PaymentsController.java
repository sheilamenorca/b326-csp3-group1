package com.joysistvi.CyberAccess.controller;

import com.joysistvi.CyberAccess.model.Payments;
import com.joysistvi.CyberAccess.service.PaymentsServiceImpl;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentsController {
    private final PaymentsServiceImpl service;
    public PaymentsController(){this.service=new PaymentsServiceImpl();}
    public PaymentsController(PaymentsServiceImpl service){this.service=service;}
    public List<Payments> getAll() throws SQLException{return service.getAllPayments();}
    public Payments getById(int id)throws SQLException{return service.getPaymentById(id).orElse(null);}
    public List<Payments> getByTransaction(int id)throws SQLException{return service.getPaymentsByTransactionId(id);}
    public List<Payments> getByUser(int id)throws SQLException{return service.getPaymentsByUserId(id);}
    public Payments add(int transactionId, BigDecimal amount, String method, String status)throws SQLException{
        Payments p=new Payments(0,transactionId,amount,method,LocalDateTime.now(),status); service.addPayment(p); return p;
    }
    public boolean update(Payments p)throws SQLException{return service.updatePayment(p);}
    public boolean setStatus(int id,String status)throws SQLException{return service.updatePaymentStatus(id,status);}
    public boolean delete(int id)throws SQLException{return service.deletePayment(id);}
}
