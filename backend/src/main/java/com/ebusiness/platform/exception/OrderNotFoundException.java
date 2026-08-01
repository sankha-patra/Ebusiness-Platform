package com.ebusiness.platform.exception;

public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
