package com.joysistvi.CyberAccess.cliview;

import com.joysistvi.CyberAccess.controller.PaymentsController;
import com.joysistvi.CyberAccess.model.Payments;
import java.sql.SQLException;
import java.util.List;

public class PaymentsView {
    private final PaymentsController controller;
    public PaymentsView(){this(new PaymentsController());}
    public PaymentsView(PaymentsController controller){this.controller=controller;}
    public void list() throws SQLException { print(controller.getAll()); }
    public void listByTransaction(int transactionId)throws SQLException{print(controller.getByTransaction(transactionId));}
    private void print(List<Payments> rows){
        String[][] data = rows.stream()
                .map(p -> new String[]{String.valueOf(p.getId()), String.valueOf(p.getTransactionId()), "PHP " + p.getAmount(),
                        String.valueOf(p.getPaymentMethod()), String.valueOf(p.getPaymentDate()), String.valueOf(p.getStatus())})
                .toArray(String[][]::new);
        UiHelper.table("Payments", new String[]{"ID", "Transaction", "Amount", "Method", "Date", "Status"}, data);
    }
}
