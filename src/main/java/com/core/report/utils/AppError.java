package com.core.report.utils;

public class AppError extends Exception {
    private int code;
    private String message;
    private Throwable err;

    public AppError(int code, String message, Throwable err) {
        this.code = code;
        this.message = message;
        this.err = err;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Throwable getErr() {
        return err;
    }

    public static AppError newInternalError(Throwable err, String... message) {
        String msg = "internal server error";
        if (message.length > 0) {
            msg = message[0];
        }
        return new AppError(500, msg, err);
    }

    public static AppError newNotFoundError(Throwable err, String... message) {
        String msg = "not found";
        if (message.length > 0) {
            msg = message[0];
        }
        return new AppError(404, msg, err);
    }

    public static AppError newBadRequestError(Throwable err, String... message) {
        String msg = "bad request";
        if (message.length > 0) {
            msg = message[0];
        }
        return new AppError(400, msg, err);
    }

    public static AppError newUnauthorizedError(Throwable err, String... message) {
        String msg = "unauthorized";
        if (message.length > 0) {
            msg = message[0];
        }
        return new AppError(401, msg, err);
    }
}
