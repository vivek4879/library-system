package com.vivek.library.exceptions;

/**
 * Thrown when an operation is attempted on a book that does not exist in the library's collection.
 * Distinct from {@link BookNotAvailableException}, which is thrown when a book exists but is currently checked out.
 * 
 */
public class BookNotFoundException extends LibraryException {
    /**
     * @param isbn the ISBN of the book that was attempted to be accessed but does not exist in the library's collection
     */
    public BookNotFoundException(String isbn) {
        super("Book not found: " + isbn);
    }
}