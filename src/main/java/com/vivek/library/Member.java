package com.vivek.library;

import java.math.BigDecimal;
import java.util.ArrayList;                                                                   
import java.util.Collections;                                 
import java.util.HashMap;                                                                               
import java.util.List;                                                                                  
import java.util.Map;


public class Member{
    private final String memberId;
    private final String name;
    private final List<CheckoutRecord> borrowingHistory;
    private final Map<String, CheckoutRecord> borrowedBooks;
    private BigDecimal fineBalance;

    public Member(String memberId, String name){
        if(memberId == null || memberId.isBlank()){
            throw new IllegalArgumentException("memberId cannot be null");
        }
        this.name = name;
        this.memberId = memberId;
        this.borrowingHistory = new ArrayList<>();
        this.borrowedBooks = new HashMap<>();
        this.fineBalance = BigDecimal.ZERO;
    }

    public String getMemberId(){
        return this.memberId;
    }

    public String getName(){
        return this.name;
    }

    public BigDecimal getFineBalance(){
        return this.fineBalance;
    }

    public Map<String,CheckoutRecord> getBorrowedBooks(){
        return Collections.unmodifiableMap(Map.copyOf(borrowedBooks));
    }

    public List<CheckoutRecord> getBorrowingHistory(){
        return Collections.unmodifiableList(List.copyOf(borrowingHistory));
    }

    void addCheckout(CheckoutRecord record){
        this.borrowedBooks.put(record.getIsbn(), record);
        this.borrowingHistory.add(record);
    }

    void removeCheckout(String isbn){
        this.borrowedBooks.remove(isbn);
    }

    void addFine(BigDecimal amount){
        this.fineBalance = this.fineBalance.add(amount); // BIgDecimal is immutable; add returns a new BigDecimal.
    }


    @Override
    public boolean equals(Object other){
        if(this == other)return true;
        if(!(other instanceof Member member))return false;
        return this.memberId.equals(member.memberId);
    }

    @Override
    public int hashCode(){
        return memberId.hashCode();
    }

    @Override
    public String toString(){
        return "Member{id='" + memberId + "', name ='" + name + "', "
            + "borrowed=" + borrowedBooks.size() + ", fineBalance=$" + fineBalance + "}"; 
    }
}
