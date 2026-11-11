package com.eduflow.backend.exception;
public class AssignmentStorageException extends RuntimeException{public AssignmentStorageException(Throwable cause){super("File storage is unavailable; please retry",cause);}}
