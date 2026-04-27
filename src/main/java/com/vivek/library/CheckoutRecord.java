package com.vivek.library;

import java.math.BigDecimal;
import java.time.LocalDate;


public class CheckoutRecord{

    private final String isbn;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private BigDecimal fineAccrued;


    public CheckoutRecord(String isbn, LocalDate checkoutDate){
        this.isbn = isbn;
        this.checkoutDate = checkoutDate;
        this.dueDate = checkoutDate.plusDays(14); //plusDays returns a new LocalDate
        returnDate = null;
        fineAccrued = BigDecimal.ZERO; // using zero isntead of new BigDecimal("0") because .ZERO is a chached singleton. Every reference points to the same object. if we create new  the nit allocates a fresh object every time its called.

    }

    public String getIsbn(){
        return this.isbn;
    }

    public LocalDate getCheckoutDate(){
        return this.checkoutDate;
    }

    public LocalDate getDueDate(){
        return this.dueDate;
    }

    public LocalDate getReturnDate(){
        return this.returnDate;
    }

    public BigDecimal getFineAccrued(){
        return this.fineAccrued;
    }

    public boolean isReturned(){
        return returnDate != null;
    }

    void markReturned(LocalDate returnDate, BigDecimal fine){
        this.returnDate = returnDate;
        this.fineAccrued = fine;
    }
}