package com.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static ResponseEntity<Map<String, String>> body(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String, String>> bad(BadRequestException e) { return body(HttpStatus.BAD_REQUEST, e.getMessage()); }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> creds(BadCredentialsException e) { return body(HttpStatus.UNAUTHORIZED, "Invalid email or password"); }

    @ExceptionHandler(DisabledException.class)
    ResponseEntity<Map<String, String>> disabled(DisabledException e) { return body(HttpStatus.FORBIDDEN, "This account is deactivated. Contact an admin."); }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<Map<String, String>> token(InvalidTokenException e) { return body(HttpStatus.UNAUTHORIZED, e.getMessage()); }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<Map<String, String>> tooMany(TooManyAttemptsException e) { return body(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> denied(AccessDeniedException e) { return body(HttpStatus.FORBIDDEN, "Access denied"); }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException e) { return body(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        var msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).distinct().collect(Collectors.joining(", "));
        return body(HttpStatus.BAD_REQUEST, msg);
    }
}
