package com.Project.Hospital_Management_System.exception;

public class NotFoundException extends ApiException {
    public NotFoundException(String msg) {
        super(ErrorCode.NOT_FOUND, msg);
    }
}
