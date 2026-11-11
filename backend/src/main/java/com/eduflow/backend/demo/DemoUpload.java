package com.eduflow.backend.demo;

import java.io.*;
import java.nio.file.*;
import org.springframework.web.multipart.MultipartFile;

/** In-memory original demo document; it still passes the normal signature/size validator. */
final class DemoUpload implements MultipartFile {
    final String name;final byte[] data;
    DemoUpload(String name,byte[] data){this.name=name;this.data=data.clone();}
    public String getName(){return "file";}public String getOriginalFilename(){return name;}
    public String getContentType(){return "application/pdf";}public boolean isEmpty(){return data.length==0;}
    public long getSize(){return data.length;}public byte[] getBytes(){return data.clone();}
    public InputStream getInputStream(){return new ByteArrayInputStream(data);}
    public void transferTo(File dest)throws IOException{Files.write(dest.toPath(),data,StandardOpenOption.CREATE_NEW);}
}
