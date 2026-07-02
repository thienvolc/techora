package com.techora.payment.application.exception;

public class InvalidVnPayPayloadException extends RuntimeException {
    public InvalidVnPayPayloadException(RuntimeException ex) {
        super(ex);
    }
}
