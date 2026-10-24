package com.apidependencyradar.application;

public class DuplicateApiDependencyException extends RuntimeException {

    public DuplicateApiDependencyException(String message) {
        super(message);
    }
}