package com.vivek.library;

import com.vivek.library.exceptions.BorrowLimitExceededException;
import com.vivek.library.exceptions.OutstandingFinesException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;


/**
 * Demonstrates the library system end-to-end with deterministic, scripted output.
 * 
 * The demo uses fixed clocks injected into {@link library} so output is
 * reproducible on any real-world date. It exercies:
 * <ul>
 *   <li> Cataog setup (5 books) and member registration (2 members).<li>
 *  <l1> Happy path checkout up to the borrow limit.<li>
 *  <li> Borrow limit enforcement(4th cehckout rejected).<li>
 *  <li> Overdue fine accrual after simulated time advance.<li>
 *  <li> Fine-limit enforcement (checkout blocked while fine exceeds ${@code 10.00}).<li>
 *  <li> Return flow and fine lock-in(fine stops accruing after return).<li>
 * </ul>
 */
public class Main {

    /**
     * Runs the scripted demo. Arguments re ignored.
     * 
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args){
        System.out.println("=== Welcome to the Library System ===\n");

        LocalDate day0 = LocalDate.of(2025, 1, 1);
        Library library = new Library(fixedClock(day0));

        System.out.println("--- Adding books to the library ---");
        library.addBook(new Book("ISBN-001", "The Great Gatsby", "F. Scott Fitzgerald"));
        library.addBook(new Book("ISBN-002", "To Kill a Mockingbird", "Harper Lee"));
        library.addBook(new Book("ISBN-003", "1984", "George Orwell"));
        library.addBook(new Book("ISBN-004", "Pride and Prejudice", "Jane Austen"));
        library.addBook(new Book("ISBN-005", "The Catcher in the Rye", "J.D. Salinger"));
        System.out.println("5 books added to the library.\n");

        System.out.println("--- Registering members ---");
        library.registerMember(new Member("M001", "Timon"));
        library.registerMember(new Member("M002", "Pumbaa"));
        System.out.println("Registered: Timon (M001) and Pumbaa (M002).\n");

        System.out.println("--- Available books ---");
        library.getAvailableBooks().forEach(book -> System.out.println(" " + book));
        System.out.println();


        System.out.println("--- Timon checks out 3 books on " + day0 + " ---");
        library.checkoutBook("M001", "ISBN-001");
        library.checkoutBook("M001", "ISBN-002");
        library.checkoutBook("M001", "ISBN-003");
        System.out.println("Timon has checked out 3 books. Due date for each: " + day0.plusDays(14) + ".\n");

        System.out.println("--- Timon attempts a 4th checkout (should fail with BorrowLimitExceededException) ---");
        try {
            library.checkoutBook("M001", "ISBN-004");
        } catch (BorrowLimitExceededException e) {
            System.out.println("Expected exception: " + e.getMessage() + "\n");
        }

        System.out.println("--- Timon's borrowing history ---");
        library.getMemberBorrowingHistory("M001").forEach(record -> System.out.println(" isbn=" + record.getIsbn()
         + ", checkoutDate=" + record.getCheckoutDate() + ", due=" + record.getDueDate()
         + ", returned=" + (record.isReturned() ? record.getReturnDate() : "Not yet")));
        System.out.println();

        LocalDate day30 = LocalDate.of(2025, 1, 31);
        library.setClock(fixedClock(day30));
        System.out.println("--- Advancing time to " + day30 + " (16 days past due) ---");
        BigDecimal fine = library.calculateFine("M001");
        System.out.println("Timon's outstanding fine: $" + fine + " ( 3 books overdue by 16 days, $0.50 per day each )\n");

        System.out.println("--- Timon attempts to checkout another book with outstanding fines (should fail with OutstandingFinesException) ---");
        //we doing a try catch here because OutstandingFinesException extends LIbraryException which extends
        // runTimeException which is a n unchecked exception which means the compiler does not force you to catch it.
        // at runtime if uncaught, it propogates through the stach looking for a mathcing catch and will breka the program wiht a stack trace in the output

        try {
            library.checkoutBook("M001", "ISBN-004");
        } catch (OutstandingFinesException e) {
            System.out.println("Checkout Blocked: " + e.getMessage() + "\n");
        }

        System.out.println("--- Timon returns all 3 books (fines freeze into balance) ---");
        library.returnBook("M001", "ISBN-001");
        library.returnBook("M001", "ISBN-002");
        library.returnBook("M001", "ISBN-003");
        System.out.println("Timon has returned all books. Outstanding fine balance: $" + library.calculateFine("M001") + "\n");

        LocalDate day60 = LocalDate.of(2025, 3, 2);
        library.setClock(fixedClock(day60));
        System.out.println("--- Advancing time to " + day60 + " (fines should not increase after return) ---");
        fine = library.calculateFine("M001");
        System.out.println("Timon's outstanding fine (should be unchanged): $" + fine + "\n");

        System.out.println("--- Timon's borrowing history after returns ---");
        library.getMemberBorrowingHistory("M001").forEach(record -> System.out.println(" isbn=" + record.getIsbn() 
        + " returned=" + record.getReturnDate()
        + " fineAccrued=$" + record.getFineAccrued()));
        System.out.println("\n=== Demo complete ===");

    }
        private static Clock fixedClock(LocalDate date) {
            return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        }
    }