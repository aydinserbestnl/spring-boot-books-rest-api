package com.love2code.books.exception;

//Exception is going to be the actual exception that we throw when a book is not found.
//
//So we can say the book not found.
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }

    public BookNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BookNotFoundException(Throwable cause) {
        super(cause);
    }
}
