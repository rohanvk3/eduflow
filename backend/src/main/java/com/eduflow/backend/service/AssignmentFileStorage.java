package com.eduflow.backend.service;
/** Private binary storage. Keys never appear in public DTOs. A cloud adapter can implement this interface. */
public interface AssignmentFileStorage {
 String put(byte[] bytes);
 byte[] read(String key);
 void delete(String key);
}
