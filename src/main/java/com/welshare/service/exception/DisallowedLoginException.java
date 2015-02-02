package com.welshare.service.exception;

public class DisallowedLoginException extends UserException {

    private final int minutes;

    public DisallowedLoginException(String message, int minutes) {
        super(message);
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }
}
