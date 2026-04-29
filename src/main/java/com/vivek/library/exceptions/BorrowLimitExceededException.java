package com.vivek.library.exceptions;

public class BorrowLimitExceededException extends LibraryException {

    public BorrowLimitExceededException(String memberId) {
        super("Borrow limit of 3 reached for member: " + memberId);
    }
}
