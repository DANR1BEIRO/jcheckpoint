package com.jcheckpoint.exception;

public class SaveSyncException extends RuntimeException{

    public SaveSyncException(String message) {
        super(message);
    }

    public SaveSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
