package com.vivek.library.exceptions;

import java.math.BigDecimal;

public class OutstandingFinesException extends LibraryException {

    public OutstandingFinesException(String memberId, BigDecimal fine) {
        super("Member " + memberId + " has outstanding fines of $" + fine);
    }
}
