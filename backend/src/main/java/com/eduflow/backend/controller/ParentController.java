package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ParentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/parents")
public class ParentController {
 final ParentManagementService management;final ParentPortalService portal;final FinanceReceiptService receipts;
 public ParentController(ParentManagementService management,ParentPortalService portal,FinanceReceiptService receipts){this.management=management;this.portal=portal;this.receipts=receipts;}
 @PostMapping public ResponseEntity<ProfileView> create(@Valid @RequestBody CreateProfile input){var p=management.create(input);return ResponseEntity.created(URI.create("/api/parents/"+p.id())).body(p);}
 @GetMapping public PageResponse<ProfileView> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)StudentStatus status){return management.directory(page,size,search,status);}
 @GetMapping("/{id}")public ProfileView profile(@PathVariable Long id){return management.get(id);}
 @PutMapping("/{id}")public ProfileView update(@PathVariable Long id,@Valid @RequestBody AdminProfile input){return management.update(id,input);}
 @GetMapping("/candidates/users")public PageResponse<UserChoice> candidates(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String search){return management.candidates(page,size,search);}
 @GetMapping("/me")public ProfileView mine(){return management.mine();}
 @PutMapping("/me")public ProfileView updateMine(@Valid @RequestBody EditProfile input){return management.updateMine(input);}
 @PostMapping("/links")public ResponseEntity<LinkView> link(@Valid @RequestBody LinkRequest input){var l=management.createLink(input);return ResponseEntity.created(URI.create("/api/parents/links/"+l.id()+"/history")).body(l);}
 @GetMapping("/links")public PageResponse<LinkView> links(@RequestParam(required=false)Long parentId,@RequestParam(required=false)Long studentId,@RequestParam(required=false)ParentLinkStatus status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return management.links(parentId,studentId,status,page,size);}
 @PostMapping("/links/{id}/verify")public LinkView verify(@PathVariable Long id,@Valid @RequestBody LinkAction input){return management.transition(id,input,true);}
 @PostMapping("/links/{id}/revoke")public LinkView revoke(@PathVariable Long id,@Valid @RequestBody LinkAction input){return management.transition(id,input,false);}
 @GetMapping("/links/{id}/history")public PageResponse<AuditView> history(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return management.history(id,page,size);}
 @GetMapping("/me/children")public PageResponse<ChildView> children(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String search){return portal.children(page,size,search);}
 @GetMapping("/dashboard")public Dashboard dashboard(@RequestParam(required=false)Long studentId,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate){return portal.dashboard(studentId,fromDate,toDate);}
 @GetMapping("/children/{studentId}")public ChildView child(@PathVariable Long studentId){return portal.child(studentId);}
 @GetMapping("/children/{studentId}/dashboard")public Dashboard childDashboard(@PathVariable Long studentId,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate){return portal.dashboard(studentId,fromDate,toDate);}
 @GetMapping("/children/{studentId}/attendance")public AttendanceDtos.Personal attendance(@PathVariable Long studentId,@RequestParam(required=false)Long batchId,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="0")int batchPage){return portal.attendance(studentId,batchId,fromDate,toDate,page,size,batchPage);}
 @GetMapping("/children/{studentId}/fees")public Fees fees(@PathVariable Long studentId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return portal.fees(studentId,page,size);}
 @GetMapping("/children/{studentId}/payments/{paymentId}/receipt") @PreAuthorize("hasRole('PARENT')")
 public ResponseEntity<byte[]> receipt(@PathVariable Long studentId,@PathVariable Long paymentId){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=receipt-"+paymentId+".pdf").header("X-Content-Type-Options","nosniff").body(receipts.parentReceipt(studentId,paymentId));}
 @GetMapping("/children/{studentId}/results")public PageResponse<ResultView> results(@PathVariable Long studentId,@RequestParam(required=false)Long courseId,@RequestParam(required=false)Long batchId,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return portal.results(studentId,courseId,batchId,fromDate,toDate,page,size);}
 @GetMapping("/children/{studentId}/assignments")public PageResponse<AssignmentView> assignments(@PathVariable Long studentId,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long batchId,@RequestParam(required=false)String status,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return portal.assignments(studentId,search,batchId,status,fromDate,toDate,page,size);}
 @GetMapping("/children/{studentId}/resources")public PageResponse<ResourceView> resources(@PathVariable Long studentId,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long courseId,@RequestParam(required=false)Long batchId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return portal.resources(studentId,search,courseId,batchId,page,size);}
 @GetMapping("/children/{studentId}/progress")public Progress progress(@PathVariable Long studentId,@RequestParam(required=false)LocalDate fromDate,@RequestParam(required=false)LocalDate toDate){return portal.progress(studentId,fromDate,toDate);}
}
