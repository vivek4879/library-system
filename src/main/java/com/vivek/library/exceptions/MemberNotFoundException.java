package com.vivek.library.exceptions;


/**
 * Thrown when an operation is attempted on a member that does not exist in the library's records.
 * The exception message includes the ID of the member that was not found.
 */
public class MemberNotFoundException extends LibraryException {

    /**
     * @param memberId the ID of the member that was attempted to be accessed but does not exist in the library's records
     */
    public MemberNotFoundException(String memberId) {
        super("Member not found: " + memberId);
    }
}