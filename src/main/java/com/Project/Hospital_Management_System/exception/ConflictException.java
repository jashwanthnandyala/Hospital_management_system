package com.Project.Hospital_Management_System.exception;

public class ConflictException extends ApiException {
    public ConflictException(String msg) {
        super(ErrorCode.CONFLICT, msg);
    }
}
