package com.kinhduanpc.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public AppException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static AppException notFound(String resource) {
        return new AppException(resource.toUpperCase() + "_NOT_FOUND",
            resource + " không tồn tại", HttpStatus.NOT_FOUND);
    }

    public static AppException badRequest(String code, String message) {
        return new AppException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static AppException unauthorized(String message) {
        return new AppException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
    }

    public static AppException forbidden(String message) {
        return new AppException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public static AppException conflict(String code, String message) {
        return new AppException(code, message, HttpStatus.CONFLICT);
    }
}
