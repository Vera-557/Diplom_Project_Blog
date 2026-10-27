package com.top511.vera.blog.exception;

public class DataAccessException extends RuntimeException {
//это не ошибка, а катастрофа
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}