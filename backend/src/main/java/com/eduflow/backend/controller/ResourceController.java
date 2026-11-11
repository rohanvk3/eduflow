package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ResourceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/resources") @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class ResourceController {
 final ResourceOrganizationService organization;final StudyMaterialService materials;final AssignmentFileValidator validator;
 public ResourceController(ResourceOrganizationService organization,StudyMaterialService materials,AssignmentFileValidator validator){this.organization=organization;this.materials=materials;this.validator=validator;}
 @GetMapping("/upload-policy")public Map<String,Object> policy(){return Map.of("maxFileBytes",validator.maxFileBytes(),"extensions",List.of("pdf","docx","pptx","xlsx","txt","png","jpg","jpeg"));}
 @GetMapping("/options")public Map<String,List<ScopeView>> options(){return materials.options();}
 @PostMapping("/categories")public ResponseEntity<CategoryView> createCategory(@Valid @RequestBody CategoryRequest input){var c=organization.saveCategory(null,input);return ResponseEntity.created(URI.create("/api/resources/categories/"+c.id())).body(c);}
 @GetMapping("/categories")public PageResponse<CategoryView> categories(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)StudentStatus status){return organization.categories(page,size,search,status);}
 @GetMapping("/categories/{id}")public CategoryView category(@PathVariable Long id){return organization.category(id);}
 @PutMapping("/categories/{id}")public CategoryView updateCategory(@PathVariable Long id,@Valid @RequestBody CategoryRequest input){return organization.saveCategory(id,input);}
 @PatchMapping("/categories/{id}/deactivate")public CategoryView deactivate(@PathVariable Long id){return organization.deactivate(id);}
 @PostMapping("/folders")public ResponseEntity<FolderView> createFolder(@Valid @RequestBody FolderRequest input){var f=organization.saveFolder(null,input);return ResponseEntity.created(URI.create("/api/resources/folders/"+f.id())).body(f);}
 @GetMapping("/folders")public PageResponse<FolderView> folders(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long courseId,@RequestParam(required=false)Long batchId,@RequestParam(required=false)Long parentFolderId,@RequestParam(defaultValue="false")boolean rootOnly,@RequestParam(defaultValue="false")boolean courseWideOnly){return organization.folders(page,size,search,courseId,batchId,parentFolderId,rootOnly,courseWideOnly);}
 @GetMapping("/folders/{id}")public FolderView folder(@PathVariable Long id){return organization.folder(id);}
 @PutMapping("/folders/{id}")public FolderView updateFolder(@PathVariable Long id,@Valid @RequestBody FolderRequest input){return organization.saveFolder(id,input);}
 @DeleteMapping("/folders/{id}")public ResponseEntity<Void> deleteFolder(@PathVariable Long id){organization.deleteFolder(id);return ResponseEntity.noContent().build();}
 @PostMapping(value="/materials",consumes=MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<MaterialView> create(@Valid @RequestBody MaterialRequest input){return created(materials.save(null,input,null));}
 @PostMapping(value="/materials",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<MaterialView> createFile(@Valid @RequestPart("data")MaterialRequest input,@RequestPart(value="file",required=false)MultipartFile file){return created(materials.save(null,input,file));}
 private ResponseEntity<MaterialView> created(MaterialView m){return ResponseEntity.created(URI.create("/api/resources/materials/"+m.id())).body(m);}
 @GetMapping("/materials")public PageResponse<MaterialView> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="12")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long courseId,@RequestParam(required=false)Long batchId,@RequestParam(required=false)Long categoryId,@RequestParam(required=false)Long folderId,@RequestParam(required=false)ResourceType resourceType,@RequestParam(required=false)ResourceVisibility visibility,@RequestParam(required=false)Long uploadedBy,@RequestParam(defaultValue="createdAt,desc")String sort){return materials.list(page,size,search,courseId,batchId,categoryId,folderId,resourceType,visibility,uploadedBy,sort);}
 @GetMapping("/materials/{id}")public MaterialView get(@PathVariable Long id){return materials.get(id);}
 @PutMapping("/materials/{id}")public MaterialView update(@PathVariable Long id,@Valid @RequestBody MaterialRequest input){return materials.save(id,input,null);}
 @PostMapping("/materials/{id}/publish")public MaterialView publish(@PathVariable Long id,@Valid @RequestBody VersionRequest input){return materials.publish(id,input.version());}
 @PostMapping("/materials/{id}/archive")public MaterialView archive(@PathVariable Long id,@Valid @RequestBody VersionRequest input){return materials.archive(id,input.version());}
 @GetMapping("/materials/{id}/versions")public List<VersionView> versions(@PathVariable Long id){return materials.versions(id);}
 @PostMapping("/materials/{id}/versions")public MaterialView upload(@PathVariable Long id,@RequestParam Long version,@RequestPart("file")MultipartFile file,@RequestParam(required=false)String changeNotes){return materials.upload(id,version,file,changeNotes);}
 @GetMapping("/materials/{id}/versions/{versionId}/download")public ResponseEntity<byte[]> versionDownload(@PathVariable Long id,@PathVariable Long versionId){return binary(materials.download(id,versionId,false),false);}
 @GetMapping("/materials/{id}/download")public ResponseEntity<byte[]> download(@PathVariable Long id){return binary(materials.download(id,null,false),false);}
 @GetMapping("/materials/{id}/preview")public ResponseEntity<byte[]> preview(@PathVariable Long id){return binary(materials.download(id,null,true),true);}
 @GetMapping("/me") @PreAuthorize("hasRole('STUDENT')")public PageResponse<MaterialView> mine(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="12")int size){return materials.list(page,size,"",null,null,null,null,null,null,null,"createdAt,desc");}
 @GetMapping("/stats")public Statistics stats(){return materials.stats();}
 private ResponseEntity<byte[]> binary(StudyMaterialService.Download f,boolean inline){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(f.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,(inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(f.name(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").body(f.bytes());}
}
