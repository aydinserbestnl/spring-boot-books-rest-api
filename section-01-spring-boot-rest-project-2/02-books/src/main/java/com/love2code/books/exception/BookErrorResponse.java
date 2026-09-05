package com.love2code.books.exception;

/*
This is going to be the object that we return to a user when an error is caught.
So now we have the object that we actually want to return back to the user.
 */
public class BookErrorResponse {
    //So really, the three things that we want for the user to see if an error happens anywhere in our application
    private int status;
    private String message;
    private long timestamp ;

    public BookErrorResponse(int status, String message, long timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
