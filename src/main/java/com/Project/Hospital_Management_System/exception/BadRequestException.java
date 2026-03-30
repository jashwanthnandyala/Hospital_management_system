package com.Project.Hospital_Management_System.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String msg) {
        super(ErrorCode.BAD_REQUEST, msg);
    }
}
