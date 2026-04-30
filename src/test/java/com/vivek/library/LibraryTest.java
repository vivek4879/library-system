package com.vivek.library;

import com.vivek.library.exceptions.MemberNotFoundException;
import com.vivek.library.exceptions.BookNotAvailableException;
import com.vivek.library.exceptions.BookNotFoundException;
import com.vivek.library.exceptions.BorrowLimitExceededException;
import com.vivek.library.exceptions.OutstandingFinesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LibraryTest {

    private Library library;

//BeforeEach runs before every single test in the class.
//there is also beforeAll (run once before any test, must be static) for expensive one time setup
//like spinning up a db.

//we doing beforeeach because each test should pass or fail on its own merit.why?
//Junit does not guarantee test execution order.
//modern test runners run tests concurrently. so could lead to race conditins.
//beforeeach gives test isolation, so parallelism or prior failures callnot poison later tests.

    @BeforeEach
    void setUp() {
        library = new Library();
    }

//Our calculateFine needs a localDate.now(clock), thats not a problem in prod because in prod 
//the clock is real so today advances every day; but in test we need to time travel and have the 
//ability to give today a value which you control.
//for this we use Clock.fixed whihc takes in an instant and zoneId and returns a date.
//clock.fixed lets me pin today to a specific date so i can deterministically test.
    private Clock fixedClock(LocalDate date) {
        return Clock.fixed(Instant.from(date.atStartOfDay(ZoneId.systemDefault())), ZoneId.systemDefault());
    }

    @Test
    void addBook_newBookIsStored() {
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        assertTrue(library.getAvailableBooks().contains(book));
    }

    @Test
    void addBook_duplicateIsbnRejected() {
        Book book1 = new Book("ISBN-2", "Title1", "Author1");
        Book book2 = new Book("ISBN-2", "Title2", "Author2");
        
        library.addBook(book1);
//assert throws check if the correct exceptioono was throws as well as if am exception was thwrown at all
//assertThrow does not take in two arguments, thats why we are doing it lazily using lambda functions.
//Lambdas pass code as data. Eager evaluation passes the result of code; lazy evaluation passes 
//the code itself, to be run later by the receiver.
//asserThrows needs the code itselfbecause it has to install a try/catch before the code runs.
        assertThrows(IllegalArgumentException.class, () -> library.addBook(book2));
    }

    @Test
    void registerMember_newMemberStored() {
        Member member = new Member("M-1", "John Doe");
        library.registerMember(member);
        
        // No exception thrown means registration worked
        assertDoesNotThrow(() -> library.getMemberBorrowingHistory("M-1"));
    }

    @Test
    void registerMember_duplicateIdRejected() {
        Member member1 = new Member("M-2", "Jane Smith");
        Member member2 = new Member("M-2", "John Smith");
        
        library.registerMember(member1);
        assertThrows(IllegalArgumentException.class, () -> library.registerMember(member2));
    }

    @Test
    void getAvailableBooks_excludesUnavailable() {
        Book book1 = new Book("ISBN-3", "Title1", "Author1");
        Book book2 = new Book("ISBN-4", "Title2", "Author2");
        
        library.addBook(book1);
        library.addBook(book2);
        book1.setAvailable(false);
        
        assertEquals(1, library.getAvailableBooks().size());
        assertTrue(library.getAvailableBooks().contains(book2));
        assertFalse(library.getAvailableBooks().contains(book1));
    }

    @Test
    void getAvailableBooks_returnsImmutableSnapshot() {
        Book book = new Book("ISBN-5", "Title", "Author");
        library.addBook(book);
        
        var result = library.getAvailableBooks();
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }

    @Test
    void getMemberBorrowingHistory_returnsEmptyForNewMember() {
        Member member = new Member("M-3", "Bob Wilson");
        library.registerMember(member);
        
        var history = library.getMemberBorrowingHistory("M-3");
        assertTrue(history.isEmpty());
    }

    @Test
    void getMemberBorrowingHistory_throwsForUnknownMember() {
        assertThrows(MemberNotFoundException.class, () -> library.getMemberBorrowingHistory("UNKNOWN"));
    }

    @Test
    void calculateFine_throwsForUnknownMember() {
        assertThrows(MemberNotFoundException.class, () -> library.calculateFine("GHOST"));
    }

    @Test
    void calculateFine_isZeroForMemberWithNoBorrows() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        
        assertEquals(BigDecimal.ZERO, library.calculateFine("M-1"));
    }

    @Test
    void calculateFine_isZeroOnDueDateExact() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addCheckout(new CheckoutRecord("ISBN-1", LocalDate.of(2025, 1, 1)));
        library.setClock(fixedClock(LocalDate.of(2025, 1, 15)));
        
        BigDecimal result = library.calculateFine("M-1");
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateFine_isFiftyCentsOnDayAfterDue() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addCheckout(new CheckoutRecord("ISBN-1", LocalDate.of(2025, 1, 1)));
        library.setClock(fixedClock(LocalDate.of(2025, 1, 16)));
        
        BigDecimal result = library.calculateFine("M-1");
        //compareTo comparees vale only no scale and returns 0 if equal. 
        //if we had done equals it considers scale too for bigDecimal.
        //bigdecimal is one of the few jdk classes where compareTo and equals disagree.
        //comparesTo ignore scale;equals doesnt.
        assertEquals(0, new BigDecimal("0.50").compareTo(result));
    }

    @Test
    void calculateFine_tenDaysLateIsFiveDollars() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addCheckout(new CheckoutRecord("ISBN-1", LocalDate.of(2025, 1, 1)));
        library.setClock(fixedClock(LocalDate.of(2025, 1, 25)));
        
        BigDecimal result = library.calculateFine("M-1");
        assertEquals(0, new BigDecimal("5.00").compareTo(result));
    }

    @Test
    void calculateFine_sumsAcrossMultipleOverdueBooks() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addCheckout(new CheckoutRecord("ISBN-1", LocalDate.of(2025, 1, 1))); // due Jan 15
        alice.addCheckout(new CheckoutRecord("ISBN-2", LocalDate.of(2025, 1, 5))); // due Jan 19
        library.setClock(fixedClock(LocalDate.of(2025, 2, 1))); // 17 days late for ISBN-1, 13 days late for ISBN-2
        
        BigDecimal result = library.calculateFine("M-1");
        assertEquals(0, new BigDecimal("15.00").compareTo(result)); // 8.50 + 6.50 = 15.00
    }

    @Test
    void calculateFine_combinesBalanceAndCurrentAccrual() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addFine(new BigDecimal("7.00"));
        alice.addCheckout(new CheckoutRecord("ISBN-1", LocalDate.of(2025, 1, 1)));
        library.setClock(fixedClock(LocalDate.of(2025, 1, 25))); // 10 days late = $5.00 accrual
        
        BigDecimal result = library.calculateFine("M-1");
        assertEquals(0, new BigDecimal("12.00").compareTo(result)); // 7.00 + 5.00 = 12.00
    }

    // checkoutBook tests

    @Test
    void checkoutBook_happyPath() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        library.checkoutBook("M-1", "ISBN-1");
        
        assertFalse(book.isAvailable());
        assertEquals(1, member.getBorrowedBooks().size());
        assertEquals(1, member.getBorrowingHistory().size());
    }

    @Test
    void checkoutBook_throwsMemberNotFoundForUnknownMember() {
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        assertThrows(MemberNotFoundException.class, () -> library.checkoutBook("UNKNOWN", "ISBN-1"));
    }

    @Test
    void checkoutBook_throwsBookNotFoundForUnknownBook() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        
        assertThrows(BookNotFoundException.class, () -> library.checkoutBook("M-1", "UNKNOWN"));
    }

    @Test
    void checkoutBook_allowsWhenFineExactlyTenDollars() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addFine(new BigDecimal("10.00"));
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        assertDoesNotThrow(() -> library.checkoutBook("M-1", "ISBN-1"));
    }

    @Test
    void checkoutBook_throwsOutstandingFinesAboveTenDollars() {
        Member alice = new Member("M-1", "Alice");
        library.registerMember(alice);
        alice.addFine(new BigDecimal("10.01"));
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        assertThrows(OutstandingFinesException.class, () -> library.checkoutBook("M-1", "ISBN-1"));
    }

    @Test
    void checkoutBook_throwsWhenMemberAlreadyHasThreeBooks() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        
        Book book1 = new Book("ISBN-1", "Title1", "Author1");
        Book book2 = new Book("ISBN-2", "Title2", "Author2");
        Book book3 = new Book("ISBN-3", "Title3", "Author3");
        Book book4 = new Book("ISBN-4", "Title4", "Author4");
        
        library.addBook(book1);
        library.addBook(book2);
        library.addBook(book3);
        library.addBook(book4);
        
        library.checkoutBook("M-1", "ISBN-1");
        library.checkoutBook("M-1", "ISBN-2");
        library.checkoutBook("M-1", "ISBN-3");
        
        assertThrows(BorrowLimitExceededException.class, () -> library.checkoutBook("M-1", "ISBN-4"));
    }

    @Test
    void checkoutBook_throwsWhenBookIsUnavailable() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        book.setAvailable(false);
        
        assertThrows(BookNotAvailableException.class, () -> library.checkoutBook("M-1", "ISBN-1"));
    }

    @Test
    void checkoutBook_recordsCorrectCheckoutDate() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        library.setClock(fixedClock(LocalDate.of(2025, 6, 15)));
        
        library.checkoutBook("M-1", "ISBN-1");
        
        CheckoutRecord record = member.getBorrowingHistory().get(0);
        assertEquals(LocalDate.of(2025, 6, 15), record.getCheckoutDate());
        assertEquals(LocalDate.of(2025, 6, 29), record.getDueDate());
    }

    // returnBook tests

    @Test
    void returnBook_onTimeLeavesFineBalanceUnchanged() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 1)));
        library.checkoutBook("M-1", "ISBN-1");
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 15))); // exactly on due date
        library.returnBook("M-1", "ISBN-1");
        
        assertEquals(0, BigDecimal.ZERO.compareTo(member.getFineBalance()));
        assertTrue(book.isAvailable());
    }

    @Test
    void returnBook_lateAddsFineToBalance() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 1)));
        library.checkoutBook("M-1", "ISBN-1");
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 20))); // 5 days late
        library.returnBook("M-1", "ISBN-1");
        
        assertEquals(0, new BigDecimal("2.50").compareTo(member.getFineBalance()));
    }

    @Test
    void returnBook_freezesFineSoCalculateFineStaysConstant() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 1)));
        library.checkoutBook("M-1", "ISBN-1");
        
        library.setClock(fixedClock(LocalDate.of(2025, 1, 20))); // 5 days late
        library.returnBook("M-1", "ISBN-1");
        
        BigDecimal fineAfterReturn = library.calculateFine("M-1");
        
        library.setClock(fixedClock(LocalDate.of(2025, 2, 19))); // advance 30 more days
        BigDecimal fineAfterAdvance = library.calculateFine("M-1");
        
        assertEquals(0, fineAfterReturn.compareTo(fineAfterAdvance));
    }

    @Test
    void returnBook_throwsWhenBookWasNotBorrowedByMember() {
        Member member = new Member("M-1", "Alice");
        library.registerMember(member);
        Book book = new Book("ISBN-1", "Title", "Author");
        library.addBook(book);
        
        assertThrows(IllegalArgumentException.class, () -> library.returnBook("M-1", "ISBN-1"));
    }
}