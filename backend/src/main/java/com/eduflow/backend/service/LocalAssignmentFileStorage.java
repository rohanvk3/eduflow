package com.eduflow.backend.service;
import com.eduflow.backend.exception.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class LocalAssignmentFileStorage implements AssignmentFileStorage {
 private final Path root;
 public LocalAssignmentFileStorage(@Value("${app.assignments.storage-dir:}")String location) {
  try{
   Path configured=location.isBlank()?Path.of(System.getProperty("user.home"),".eduflow","assignment-files"):Path.of(location);
   root=Files.createDirectories(configured.toAbsolutePath().normalize()).toRealPath();
  }catch(IOException ex){throw new AssignmentStorageException(ex);}
 }
 private Path path(String key){
  if(key==null||!key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.bin"))throw new IllegalArgumentException("Invalid file key");
  Path path=root.resolve(key).normalize();if(!path.getParent().equals(root))throw new IllegalArgumentException("Invalid file key");return path;
 }
 public String put(byte[] bytes){
  String key=UUID.randomUUID()+".bin";Path file=path(key);
  try(var output=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)){output.write(bytes);return key;}
  catch(IOException ex){try{Files.deleteIfExists(file);}catch(IOException suppressed){ex.addSuppressed(suppressed);}throw new AssignmentStorageException(ex);}
 }
 public byte[] read(String key){
  Path file=path(key);
  try{
   if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new ResourceNotFoundException("Attachment file is unavailable");
   try(var input=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){return input.readAllBytes();}
  }catch(IOException ex){throw new AssignmentStorageException(ex);}
 }
 public void delete(String key){try{Files.deleteIfExists(path(key));}catch(IOException ex){throw new AssignmentStorageException(ex);}}
}
