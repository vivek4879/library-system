package com.vivek.library;

public class Book{
    private final String title;
    private final String isbn;
    private final String author;
    private boolean available;

    public Book(String isbn, String title, String author){
        if(isbn == null || isbn.isBlank()){
            throw new IllegalArgumentException("isbn Cannot be blank");
        }
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.available = true;
    }

    public String getIsbn(){
        return this.isbn;
    }

    public String getTitle(){
        return this.title;
    }

    public String getAuthor(){
        return this.author;
    }

    public boolean isAvailable(){
        return this.available;
    }

    void setAvailable(boolean available){
        this.available = available;
    }

    @Override
    public boolean equals(Object other){
        if(this == other)return true;
        if(!(other instanceof Book book))return false;
        return this.isbn.equals(book.isbn);
    }

    @Override
    public int hashCode(){
        return isbn.hashCode();
    }

    @Override
    public String toString(){
        return "Book " + this.title + " isbn " + this.isbn +  "available = " + this.available;
    }





}