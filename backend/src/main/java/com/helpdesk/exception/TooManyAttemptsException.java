package com.helpdesk.exception;
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String m) { super(m); }
}
