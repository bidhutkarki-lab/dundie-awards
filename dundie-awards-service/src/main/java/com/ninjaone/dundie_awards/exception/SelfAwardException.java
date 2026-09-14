package com.ninjaone.dundie_awards.exception;

public class SelfAwardException extends RuntimeException {

    public SelfAwardException(Long employeeId) {
        super("Employees cannot give a dundie award to themselves: employeeId=" + employeeId);
    }
}
