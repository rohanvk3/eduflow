package com.eduflow.backend.service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;
@Component
public class AssignmentFiles {
 private final AssignmentFileStorage storage;
 public AssignmentFiles(AssignmentFileStorage storage){this.storage=storage;}
 public String save(byte[] bytes){
  if(!TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("File writes require a transaction");
  String key=storage.put(bytes);
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)cleanup(key);}
  });return key;
 }
 public byte[] read(String key){return storage.read(key);}
 public void deleteAfterCommit(String key){TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){cleanup(key);}});}
 private void cleanup(String key){try{storage.delete(key);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).error("Could not clean private assignment file",ex);}}
}
