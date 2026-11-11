package com.eduflow.backend.controller;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.AssignmentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/assignments")
public class AssignmentController {
 final AssignmentService assignments;final SubmissionService submissions;final AssignmentStatisticsService statistics;final AssignmentFileValidator validator;
 public AssignmentController(AssignmentService assignments,SubmissionService submissions,AssignmentStatisticsService statistics,AssignmentFileValidator validator){this.assignments=assignments;this.submissions=submissions;this.statistics=statistics;this.validator=validator;}
 @GetMapping("/upload-policy") @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")public Map<String,Object> policy(){return Map.of("maxFileBytes",validator.maxFileBytes(),"maxFiles",10,"extensions",List.of("pdf","docx","png","jpg","jpeg"));}
 @PostMapping public ResponseEntity<AssignmentView> create(@Valid @RequestBody AssignmentRequest input){var value=assignments.save(null,input);return ResponseEntity.created(URI.create("/api/assignments/"+value.id())).body(value);}
 @GetMapping public PageResponse<AssignmentView> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long batchId,@RequestParam(required=false)AssignmentStatus status,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate,@RequestParam(defaultValue="dueDateTime,desc")String sort,@RequestParam(defaultValue="false")boolean pendingOnly){return assignments.list(page,size,search,batchId,status,fromDate,toDate,sort,pendingOnly);}
 @GetMapping("/{id}")public AssignmentView get(@PathVariable Long id){return assignments.get(id);}
 @PutMapping("/{id}")public AssignmentView update(@PathVariable Long id,@Valid @RequestBody AssignmentRequest input){return assignments.save(id,input);}
 @PostMapping("/{id}/publish")public AssignmentView publish(@PathVariable Long id,@Valid @RequestBody VersionRequest input){return assignments.publish(id,input);}
 @PostMapping("/{id}/close")public AssignmentView close(@PathVariable Long id,@Valid @RequestBody VersionRequest input){return assignments.close(id,input);}
 @GetMapping("/batches/{batchId}")public PageResponse<AssignmentView> batch(@PathVariable Long batchId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return assignments.list(page,size,"",batchId,null,null,null,"dueDateTime,desc");}
 @GetMapping("/me") @PreAuthorize("hasRole('STUDENT')")public PageResponse<AssignmentView> mine(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return assignments.list(page,size,"",null,null,null,null,"dueDateTime,desc");}
 @GetMapping("/stats")public Statistics stats(@RequestParam(required=false)Long batchId){return statistics.stats(batchId);}
 @PostMapping("/{id}/attachments")public List<FileView> upload(@PathVariable Long id,@RequestParam Long version,@RequestPart("files")List<MultipartFile> files){return assignments.upload(id,version,files);}
 @GetMapping("/{id}/attachments")public List<FileView> files(@PathVariable Long id){return assignments.attachments(id);}
 @GetMapping("/attachments/{attachmentId}/download")public ResponseEntity<byte[]> download(@PathVariable Long attachmentId){return binary(assignments.download(attachmentId));}
 @DeleteMapping("/attachments/{attachmentId}")public ResponseEntity<Void> remove(@PathVariable Long attachmentId,@RequestParam Long version){assignments.removeAttachment(attachmentId,version);return ResponseEntity.noContent().build();}
 @PostMapping(value="/{id}/submissions",consumes=MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<SubmissionView> submitText(@PathVariable Long id,@Valid @RequestBody SubmissionRequest input){return created(submissions.create(id,input,List.of()));}
 @PostMapping(value="/{id}/submissions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<SubmissionView> submitFiles(@PathVariable Long id,@Valid @RequestPart("data")SubmissionRequest input,@RequestPart(value="files",required=false)List<MultipartFile> files){return created(submissions.create(id,input,files));}
 private ResponseEntity<SubmissionView> created(SubmissionView value){return ResponseEntity.created(URI.create("/api/assignments/submissions/"+value.id())).body(value);}
 @GetMapping("/{id}/submissions")public PageResponse<SubmissionView> listSubmissions(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)SubmissionStatus status){return submissions.list(id,page,size,search,status);}
 @GetMapping("/{id}/my-submission") @PreAuthorize("hasRole('STUDENT')")public SubmissionView own(@PathVariable Long id){return submissions.ownForAssignment(id);}
 @GetMapping("/{id}/pending-recipients")public PageResponse<RecipientView> pending(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="")String search){return assignments.pendingRecipients(id,page,size,search);}
 @GetMapping("/submissions/{submissionId}")public SubmissionView submission(@PathVariable Long submissionId){return submissions.get(submissionId);}
 @PutMapping(value="/submissions/{submissionId}",consumes=MediaType.APPLICATION_JSON_VALUE)
 public SubmissionView editText(@PathVariable Long submissionId,@Valid @RequestBody SubmissionRequest input){return submissions.update(submissionId,input,List.of());}
 @PutMapping(value="/submissions/{submissionId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public SubmissionView editFiles(@PathVariable Long submissionId,@Valid @RequestPart("data")SubmissionRequest input,@RequestPart(value="files",required=false)List<MultipartFile> files){return submissions.update(submissionId,input,files);}
 @GetMapping("/me/submissions")public PageResponse<SubmissionView> ownSubmissions(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)SubmissionStatus status){return submissions.mine(page,size,status);}
 @PostMapping("/submissions/{submissionId}/attachments")public List<FileView> uploadSubmission(@PathVariable Long submissionId,@RequestParam Long version,@RequestPart("files")List<MultipartFile> files){return submissions.upload(submissionId,version,files);}
 @GetMapping("/submissions/{submissionId}/attachments")public List<FileView> submissionFiles(@PathVariable Long submissionId){return submissions.attachments(submissionId);}
 @GetMapping("/submission-attachments/{attachmentId}/download")public ResponseEntity<byte[]> downloadSubmission(@PathVariable Long attachmentId){return binary(submissions.download(attachmentId));}
 @PutMapping("/submissions/{submissionId}/grade")public SubmissionView grade(@PathVariable Long submissionId,@Valid @RequestBody GradeRequest input){return submissions.grade(submissionId,input);}
 @GetMapping("/submissions/{submissionId}/history")public History history(@PathVariable Long submissionId){return submissions.history(submissionId);}
 private ResponseEntity<byte[]> binary(AssignmentService.Download file){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").body(file.bytes());}
}
