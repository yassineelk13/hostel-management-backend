// ✅ NOUVEAU : ValidationException.java
package com.hostel.management.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}