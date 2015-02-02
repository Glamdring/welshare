package com.welshare.service.exception;

public class UserException extends Exception {

    public static final UserException EMAIL_PROBLEM = new UserException(
            "emailProblem");

    public static final UserException INCORRECT_LOGIN_DATA = new UserException(
            "incorrectLoginData");

    public static final UserException USER_INACTIVE = new UserException(
            "userInactive");

    public static final UserException USER_ALREADY_ACTIVE = new UserException(
            "userAlreadyActive");

    public static final UserException INVALID_ACTIVATION_CODE = new UserException(
            "invalidActivationCode");

    public static final UserException UNEXPECTED_PROBLEM = new UserException(
            "unexpectedProblem");

    public static final UserException INVALID_EMAIL = new UserException(
            "invalidEmail");

    public static final UserException USER_ALREADY_EXISTS = new UserException(
            "userAlreadyExists");

    public static final UserException EMAIL_ALREADY_EXISTS = new UserException(
            "emailAlreadyExists");

    public UserException(String messageKey) {
        super(messageKey);
    }
}