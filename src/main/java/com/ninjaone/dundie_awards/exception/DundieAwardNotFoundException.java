package com.ninjaone.dundie_awards.exception;

public class DundieAwardNotFoundException extends RuntimeException {

    public DundieAwardNotFoundException(Long id) {
        super("Dundie award not found: " + id);
    }
}
