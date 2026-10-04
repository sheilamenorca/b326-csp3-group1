package com.joysistvi.CyberAccess.service;

import com.joysistvi.CyberAccess.model.Payments;
import com.joysistvi.CyberAccess.repo.PaymentsRepo;
import com.joysistvi.CyberAccess.repo.PaymentsRepoImpl;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PaymentsServiceImpl {
    private final PaymentsRepo paymentsRepo;
    public PaymentsServiceImpl(){this(new PaymentsRepoImpl());}
    public PaymentsServiceImpl(PaymentsRepo paymentsRepo){this.paymentsRepo=paymentsRepo;}
    public List<Payments> getAllPayments() throws SQLException{return paymentsRepo.findAll();}
    public Optional<Payments> getPaymentById(int id)throws SQLException{return paymentsRepo.findById(Validation.id(id));}
    public List<Payments> getPaymentsByTransactionId(int id)throws SQLException{return paymentsRepo.findByTransactionId(Validation.id(id));}
    public List<Payments> getPaymentsByUserId(int id)throws SQLException{return paymentsRepo.findByUserId(Validation.id(id));}
    public List<Payments> getPaymentsByStatus(String status)throws SQLException{return paymentsRepo.findByStatus(Validation.text(status,"Status",30));}
    public boolean paymentExists(int id)throws SQLException{return paymentsRepo.existsById(Validation.id(id));}
    public void addPayment(Payments p)throws SQLException{validate(p);paymentsRepo.save(p);}
    public boolean updatePayment(Payments p)throws SQLException{validate(p);return paymentsRepo.update(p);}
    public boolean updatePaymentStatus(int id,String status)throws SQLException{return paymentsRepo.updateStatus(Validation.id(id),Validation.text(status,"Status",30));}
    public boolean deletePayment(int id)throws SQLException{return paymentsRepo.delete(Validation.id(id));}
    private void validate(Payments p){
        if(p==null)throw new IllegalArgumentException("Payment information cannot be null.");
        Validation.id(p.getTransactionId());
        Validation.decimal(p.getAmount(),"Amount",true);
        Validation.text(p.getPaymentMethod(),"Payment method",30);
        Validation.text(p.getStatus(),"Status",30);
        if(p.getPaymentDate()==null)p.setPaymentDate(java.time.LocalDateTime.now());
    }
}
