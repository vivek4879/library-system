package com.vivek.library;

import com.vivek.library.exceptions.MemberNotFoundException;
import com.vivek.library.exceptions.BookNotFoundException;
import com.vivek.library.exceptions.BookNotAvailableException;
import com.vivek.library.exceptions.BorrowLimitExceededException;
import com.vivek.library.exceptions.OutstandingFinesException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Library implements LibraryOperations{
    private final Map<String, Book> books;
    private final Map<String, Member> members;
//Why does lib take a clock instead of just calling LocakDate.now()?
// LocalDate.now() no args reads the system clock. themethod has as undeclared dependency on
// what time is it on this machine right now. which measn:
// - tests cannot control it.to test fine after 30 days you will have to either wait 30 days or
// mock the static LocalDate.now() which requires other heavy lifting.
// - tests arent deterministic.
// -dependency invisible. reading signature you will never know it deopends on wall clock.

// CLock injection fixes all:
// - dependecy declared in constructor.
// -test can inject hwat they need.
// -Prod inject what it needs.
// anything thats a  global sideeffect should be injected.
    private Clock clock;
    //Static fields belong to the class, not the instance. 
    //these exist exactly once, loaded when the JVM loads the lib class itself, before any
    //instance is constructed. Every lib instance will share the same reference for these.
    private static final BigDecimal DAILY_FINE = new BigDecimal("0.50");
    private static final BigDecimal FINE_LIMIT = new BigDecimal("10.00");
    private static final int BORROW_LIMIT = 3;


    //this has to be first line in the constructor; thsi and super mutually exclusive
    //if nothing then super() is implicit.
    //this() is not a method call; it says before this constructor body runs, run that o
    //other costructor first.
    public Library(){
        this(Clock.systemDefaultZone());

    }
    public Library(Clock clock){
        this.books = new HashMap<>();
        this.members = new HashMap<>();
        this.clock = clock;

    }

//When we create Library we pass in a clock, we wire it to that clock. wiring should be immutable, once object constructed, dependencies cannot be swapped.wiring locked.
//we enforce it with making clock private final.
// immutable wiring desirable because
//1. you read the constructor and you know the object's dependencies for its lifetime.
//2. thread safety -> final field safely published to otherthreads after construction. mutable field neds synchroization.
//so immutable wiring -> cheaper concurrency.
//3. invarients hold across object's life.
//SetClock violats all three.
//immutabile wiring means dependencies locked at construction. final field + no setter. we give that up
// only wehn testability demands it.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void addBook(Book book){
        if(books.containsKey(book.getIsbn())){
            throw new IllegalArgumentException("Book with ISBN " + book.getIsbn() + " already exists");
        }
        books.put(book.getIsbn(), book);

    }

    @Override
    public void registerMember(Member member){
        if(members.containsKey(member.getMemberId())){
            throw new IllegalArgumentException("Member with ID " + member.getMemberId() + " already exists");
        }
        members.put(member.getMemberId(), member);
    }
    

    @Override
    public void checkoutBook(String memberId, String isbn){
        // We wanna fail fast.
        // ALso prevent downstream NPEs.
        // so the hierarchy we follow is:
        // existence checks first
        // Account level blockers Next 
        // Member state next 
        // Transaction level last 

        //optimizing for user experience and not cpu cycles.
        //cost diff negligible.
        Member member = members.get(memberId);
        if(member == null){
            throw new MemberNotFoundException(memberId);
        }

        Book book = books.get(isbn);
        if(book == null){
            throw new BookNotFoundException(isbn);
        }

        BigDecimal currentFine = calculateFine(memberId);

        if(currentFine.compareTo(FINE_LIMIT) > 0){
            throw new OutstandingFinesException(memberId, currentFine);
        }
         
        if(member.getBorrowedBooks().size() >= BORROW_LIMIT){
            throw new BorrowLimitExceededException(memberId);
        }

        if(!book.isAvailable()){
            throw new BookNotAvailableException(isbn);
        }

        //This two phase process gives me atomicity in signle threaded context.
        // validation phase can throw, mutation shouldnt. So if any check fails, the state of the system
        // does not change or is not stuck. all or nothing.
        // so validate the nnutate gives me transactional semantics without a transaction.
        // validate-then-mutate is atomic per thread, not across threads. the window betn validation and 
        // mutation is a race condition, fixing it needs synchronization, or the whole checkoutBook
        // wrapped in a lock.

        // JVM level failures are unrecoverable by design. Trying to handle them creates more bugs than it fixes.
        // Crash fast , restart clean.
        // OutOfMemoryError, StackOverFlowError are errors not exceptions and the convention is you
        // dont catch error.

        CheckoutRecord record = new CheckoutRecord(isbn, LocalDate.now(clock));
        member.addCheckout(record);
        book.setAvailable(false);



        
    }

    @Override
    public void returnBook(String memberId, String isbn) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new MemberNotFoundException(memberId);
    }
        Book book = books.get(isbn);
        if (book == null) {
            throw new BookNotFoundException(isbn);
        }
        CheckoutRecord record = member.getBorrowedBooks().get(isbn);
        if (record == null) {
            throw new IllegalArgumentException("Member " + memberId + " did not borrow book with ISBN " + isbn);
        }
        // Calculate fine for this book if returned late
        LocalDate today = LocalDate.now(clock);
        BigDecimal fine = computeOverDueFine(record, today);
        member.addFine(fine);
        record.markReturned(today, fine);
        member.removeCheckout(isbn);
        book.setAvailable(true);
    }

    @Override
    public List<Book> getAvailableBooks() {
        List<Book> availableBooks = new ArrayList<>();
        for (Book book : books.values()) {
            if (book.isAvailable()) {
                availableBooks.add(book);
            }
        }
        return List.copyOf(availableBooks);
    }

    @Override
    public List<CheckoutRecord> getMemberBorrowingHistory(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Member with ID " + memberId + " not found");
        }
        return List.copyOf(member.getBorrowingHistory());
    }

// Why store finBalance at all?
// 1. perfromance-> cehckoutBook calls calculatefine on every checkout. 
// if getFineBalance had to walk ntire borrowingHistory list and sum each record's fineAccrued,
// thats O(n) per checkout where n grows forever. storing aggregate makes it O(1).
// we pay a tiny write cost to make the hot read path constant time.
// 2. single source of trutht for settled debt. borrowing history is a audit log. append only record.
// fineBalance is curetn State.if we forgive the fine you adjust the fineBalance without reqriting history.

//Calculate fine not static becuse it calls clock to knwo what today is.
//CLock is an instance field of library, injected via the constructor, swappable via setCLock,
//different per library instance- prod uses system, test clock.fixed.
//static method has no this so cannot read this.clock. would have to either
// take clock as parameter- pollutes API, every called needs clock reference
// call localDate.now() directly -> kills testability which is the whole reason clock is injected in the first place.
// so calculateFIne is a instance method as it dependds on injected per instance state.
//Static is about the scope of dependencies.
    @Override
    public BigDecimal calculateFine(String memberId){
        Member member = members.get(memberId);
        if(member == null){
            throw new MemberNotFoundException("member with " + memberId + " not found");
        }

        LocalDate today = LocalDate.now(clock);
        BigDecimal currentAccrual = BigDecimal.ZERO;

        for(CheckoutRecord record:member.getBorrowedBooks().values()){
            currentAccrual = currentAccrual.add(computeOverDueFine(record, today));
        }
        return member.getFineBalance().add(currentAccrual);
    }


    //Static when method needs no instance state at all- not the fields, not other instance methods.
    //its output depends only on its args + class level constants.
    //you give computeOverDueFine its 2 args and it will calculte.no lib instance needed. you could call it without
    // ever constructing a lib.
    // Pure fuinction over args and constants. doesnt read or write nay instance state, so biding it to
    //an instance is misleading. it implies a dependency that does not exist.
    //this is static because it does not modify any field of the library. just internally calcuates.
    //static means class level, not instance level. Static method cannot acces non static fields.
    private static BigDecimal computeOverDueFine(CheckoutRecord record, LocalDate asOfDate){
        long daysLate = ChronoUnit.DAYS.between(record.getDueDate(), asOfDate);
        if(daysLate <= 0){
            return BigDecimal.ZERO;
        }
        return(BigDecimal.valueOf(daysLate).multiply(DAILY_FINE));
    }

}