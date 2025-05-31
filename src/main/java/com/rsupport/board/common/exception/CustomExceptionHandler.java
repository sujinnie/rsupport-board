package com.rsupport.board.common.exception;

import lombok.Getter;

@Getter
public class CustomExceptionHandler extends RuntimeException{
    private final ErrorCode errorCode;

    public CustomExceptionHandler(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    public CustomExceptionHandler(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomExceptionHandler(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
