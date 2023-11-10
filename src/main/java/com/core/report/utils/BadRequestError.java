package com.core.report.utils;

public class BadRequestError extends AppError {
    public BadRequestError(int code, String message, Throwable err) {
        super(code, message, err);
    }
}
