package com.eduflow.backend.service;
import com.eduflow.backend.exception.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
@Component
public class AssignmentFileValidator {
 public record ValidatedFile(String name,String contentType,byte[] bytes){}
 private final long maxBytes;
 public AssignmentFileValidator(@Value("${app.assignments.max-file-bytes:10485760}")long maxBytes){this.maxBytes=maxBytes;}
 public long maxFileBytes(){return maxBytes;}
 public ValidatedFile validateResource(MultipartFile upload){
  if(upload==null||upload.isEmpty())throw new IllegalArgumentException("A nonempty resource file is required");
  String name=upload.getOriginalFilename();
  if(name==null||name.length()>180||name.isBlank()||name.contains("/")||name.contains("\\")||name.contains(":")||name.contains("..")||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid resource filename");
  String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
  if(Set.of("pdf","docx","png","jpg","jpeg").contains(ext)){var file=validate(upload);checkMime(upload,file.contentType());return file;}
  if(!Set.of("txt","pptx","xlsx").contains(ext))throw new IllegalArgumentException("Allowed resources: PDF, DOCX, PPTX, XLSX, TXT, PNG, JPG");
  if(upload.getSize()>maxBytes)throw new AssignmentUploadTooLargeException();
  try(var input=upload.getInputStream()){
   byte[] bytes=input.readNBytes(Math.toIntExact(maxBytes+1));if(bytes.length>maxBytes)throw new AssignmentUploadTooLargeException();
   String mime;
   if(ext.equals("txt")){
    String value=java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    if(value.isBlank()||value.codePoints().anyMatch(c->Character.isISOControl(c)&&c!=9&&c!=10&&c!=13))throw new IllegalArgumentException("Text files must contain valid UTF-8 text without control characters");
    mime="text/plain";
   }else{office(bytes,ext);mime="application/vnd.openxmlformats-officedocument."+(ext.equals("pptx")?"presentationml.presentation":"spreadsheetml.sheet");}
   checkMime(upload,mime);return new ValidatedFile(name,mime,bytes);
  }catch(IOException ex){throw new IllegalArgumentException("File content does not match its declared format");}
 }
 private void checkMime(MultipartFile upload,String expected){String mime=upload.getContentType();if(mime!=null&&!mime.isBlank()&&!mime.equalsIgnoreCase("application/octet-stream")&&!mime.split(";")[0].equalsIgnoreCase(expected))throw new IllegalArgumentException("File MIME type does not match validated content");}
 public ValidatedFile validate(MultipartFile upload){
  if(upload==null||upload.isEmpty())throw new IllegalArgumentException("Empty files are not accepted");
  if(upload.getSize()>maxBytes)throw new AssignmentUploadTooLargeException();
  String name=upload.getOriginalFilename();
  if(name==null||name.isBlank()||name.length()>180||name.codePoints().anyMatch(c->Character.isISOControl(c))||name.contains("/")||name.contains("\\")||name.contains(":")||name.contains(".."))throw new IllegalArgumentException("Invalid attachment filename");
  String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
  if(!Set.of("pdf","docx","png","jpg","jpeg").contains(ext))throw new IllegalArgumentException("Allowed files: PDF, DOCX, PNG, JPG");
  try {
   byte[] bytes;
   try(var in=upload.getInputStream()){bytes=in.readNBytes(Math.toIntExact(maxBytes+1));}
   if(bytes.length>maxBytes)throw new AssignmentUploadTooLargeException();
   String type;
   switch(ext){
    case "pdf" -> {pdf(bytes);type="application/pdf";}
    case "docx" -> {docx(bytes);type="application/vnd.openxmlformats-officedocument.wordprocessingml.document";}
    default -> {image(bytes,ext);type=ext.equals("png")?"image/png":"image/jpeg";}
   }
   return new ValidatedFile(name,type,bytes);
  }catch(IOException|IllegalStateException ex){throw new IllegalArgumentException("File content is invalid or does not match its extension");}
 }
 private void image(byte[] bytes,String ext)throws IOException{
  if(ext.equals("png")){byte[] signature={(byte)137,80,78,71,13,10,26,10};if(bytes.length<8||!Arrays.equals(Arrays.copyOf(bytes,8),signature))throw new IllegalArgumentException("Invalid PNG content");}
  else if(bytes.length<3||(bytes[0]&255)!=255||(bytes[1]&255)!=216||(bytes[2]&255)!=255)throw new IllegalArgumentException("Invalid JPEG content");
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IllegalArgumentException("Invalid image");
   var reader=readers.next();
   try {reader.setInput(stream,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<=0||h<=0||w>10000||h>10000||(long)w*h>20000000)throw new IllegalArgumentException("Image dimensions are too large");String format=reader.getFormatName();if(!(ext.equals("png")?format.equalsIgnoreCase("png"):format.equalsIgnoreCase("jpeg")))throw new IllegalArgumentException("Image format does not match extension");if(reader.read(0)==null)throw new IllegalArgumentException("Invalid image");}
   finally {reader.dispose();}
  }
 }
 private void pdf(byte[] bytes)throws IOException{
  if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')throw new IllegalArgumentException("Invalid PDF content");
  try(var doc=Loader.loadPDF(bytes)){
   if(doc.isEncrypted()||doc.getNumberOfPages()<1||doc.getNumberOfPages()>2000)throw new IllegalArgumentException("PDF must be unencrypted and contain 1–2000 pages");
   var seen=Collections.newSetFromMap(new IdentityHashMap<COSBase,Boolean>());var pending=new ArrayDeque<COSBase>();pending.add(doc.getDocument().getTrailer());
   var blocked=Set.of("JavaScript","JS","Launch","EmbeddedFile","EmbeddedFiles","RichMedia","XFA","OpenAction","AA");
   while(!pending.isEmpty()){
    var value=pending.removeFirst();if(!seen.add(value))continue;if(seen.size()>200000)throw new IllegalArgumentException("PDF is too complex");
    if(value instanceof COSObject o){if(o.getObject()!=null)pending.add(o.getObject());}
    else if(value instanceof COSDictionary d){for(var entry:d.entrySet()){if(blocked.contains(entry.getKey().getName()))throw new IllegalArgumentException("Active or embedded PDF content is not allowed");if(entry.getValue()!=null)pending.add(entry.getValue());}}
    else if(value instanceof COSArray array){for(var child:array)if(child!=null)pending.add(child);}
    else if(value instanceof COSName name&&blocked.contains(name.getName()))throw new IllegalArgumentException("Active or embedded PDF content is not allowed");
   }
  }
 }
 private Document xml(byte[] bytes){
  try {
   var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
   f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
   f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");f.setXIncludeAware(false);f.setExpandEntityReferences(false);
   var parser=f.newDocumentBuilder();parser.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){@Override public void fatalError(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}});
   return parser.parse(new ByteArrayInputStream(bytes));
  }catch(Exception ex){throw new IllegalArgumentException("Invalid DOCX XML");}
 }
 private void docx(byte[] bytes)throws IOException {
  office(bytes,"docx");
 }
 private void office(byte[] bytes,String ext)throws IOException {
  String main=ext.equals("docx")?"word/document.xml":ext.equals("pptx")?"ppt/presentation.xml":"xl/workbook.xml";
  String mainType="application/vnd.openxmlformats-officedocument."+(ext.equals("docx")?"wordprocessingml.document":ext.equals("pptx")?"presentationml.presentation":"spreadsheetml.sheet")+".main+xml";
  String rootName=ext.equals("docx")?"document":ext.equals("pptx")?"presentation":"workbook";
  String namespace="http://schemas.openxmlformats.org/"+(ext.equals("docx")?"wordprocessingml":ext.equals("pptx")?"presentationml":"spreadsheetml")+"/2006/main";
  if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K'||bytes[2]!=3||bytes[3]!=4)throw new IllegalArgumentException("Invalid DOCX content");
  Set<String> names=new HashSet<>();boolean types=false,document=false,relationship=false;long total=0;
  try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    String name=entry.getName();String lower=name.toLowerCase(Locale.ROOT);
    if(names.size()>=1000||!names.add(name)||name.startsWith("/")||name.contains("\\")||name.contains(":")||Arrays.asList(name.split("/")).contains(".."))throw new IllegalArgumentException("Unsafe DOCX archive");
    if(lower.contains("vbaproject")||lower.contains("/embeddings/")||lower.contains("/activex/")||lower.endsWith(".exe")||lower.endsWith(".dll")||lower.endsWith(".bin"))throw new IllegalArgumentException("Embedded or executable Office content is not allowed");
    if(entry.isDirectory())continue;
    var out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
    while((n=zip.read(buffer))!=-1){total+=n;if(total>41943040||out.size()+n>10485760)throw new IllegalArgumentException("DOCX archive expands beyond the allowed limit");out.write(buffer,0,n);}
    if(lower.endsWith(".xml")||lower.endsWith(".rels")){
     var doc=xml(out.toByteArray());
     if(name.equals("[Content_Types].xml")){var nodes=doc.getElementsByTagNameNS("*","Override");for(int i=0;i<nodes.getLength();i++){var item=(org.w3c.dom.Element)nodes.item(i);if(("/"+main).equals(item.getAttribute("PartName"))&&mainType.equals(item.getAttribute("ContentType")))types=true;if(item.getAttribute("ContentType").toLowerCase(Locale.ROOT).contains("macro"))throw new IllegalArgumentException("Macros are not allowed");}}
     if(name.equals(main))document=rootName.equals(doc.getDocumentElement().getLocalName())&&namespace.equals(doc.getDocumentElement().getNamespaceURI());
     if(lower.endsWith(".rels")){var nodes=doc.getElementsByTagNameNS("*","Relationship");for(int i=0;i<nodes.getLength();i++){var item=(org.w3c.dom.Element)nodes.item(i);if("External".equalsIgnoreCase(item.getAttribute("TargetMode")))throw new IllegalArgumentException("External Office relationships are not allowed");if(name.equals("_rels/.rels")&&item.getAttribute("Type").endsWith("/officeDocument")&&main.equals(item.getAttribute("Target")))relationship=true;}}
    }
   }
  }
  if(!types||!document||!relationship)throw new IllegalArgumentException("File is not a valid DOCX document");
 }
}
