package com.vivek.library.exceptions;


/**
 * Thrown when a checkout is attempted on a book that is not currently available.
 * The exception message includes the ISBN of the unavailable book.
 */
public class BookNotAvailableException extends LibraryException {

    /**
     * @param isbn the ISBN of the book that was attempted to be checked out but is not available
     */
    public BookNotAvailableException(String isbn) {
        super("Book not available: " + isbn);
    }
}
