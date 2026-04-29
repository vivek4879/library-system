package com.vivek.library;

import java.math.BigDecimal;
import java.util.List;

public interface LibraryOperations{

    void addBook(Book book);
    void registerMember(Member member);
    void checkoutBook(String memberId, String isbn) ;
    void returnBook(String memberId, String isbn);
    List<Book> getAvailableBooks();
    List<CheckoutRecord> getMemberBorrowingHistory(String memberId);
    BigDecimal calculateFine(String memberId);
}