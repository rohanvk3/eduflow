package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ExamDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/exams")
public class ExamController {
 final ExamService exams;final ExamAttemptService attempts;final ExamResultService results;final ExamAnalyticsService analytics;
 public ExamController(ExamService exams,ExamAttemptService attempts,ExamResultService results,ExamAnalyticsService analytics){this.exams=exams;this.attempts=attempts;this.results=results;this.analytics=analytics;}
 @PostMapping public ResponseEntity<ExamView> create(@Valid @RequestBody ExamRequest r){var e=exams.save(null,r);return ResponseEntity.created(URI.create("/api/exams/"+e.id())).body(e);}
 @GetMapping public PageResponse<ExamView> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="")String search,@RequestParam(required=false)Long batchId,@RequestParam(required=false)ExamType examType,@RequestParam(required=false)ExamStatus status,@RequestParam(defaultValue="startDateTime,desc")String sort){return exams.list(page,size,search,batchId,examType,status,sort);}
 @GetMapping("/batches/{batchId}")public PageResponse<ExamView> batch(@PathVariable Long batchId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return exams.list(page,size,"",batchId,null,null,"startDateTime,desc");}
 @GetMapping("/me") @PreAuthorize("hasRole('STUDENT')") public PageResponse<ExamView> mine(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return exams.list(page,size,"",null,null,null,"startDateTime,desc");}
 @GetMapping("/analytics")public Analytics analytics(@RequestParam(required=false)Long batchId,@RequestParam(required=false)Long studentId){return analytics.analytics(batchId,studentId);}
 @GetMapping("/{id}")public ExamView get(@PathVariable Long id){return exams.get(id);}
 @PutMapping("/{id}")public ExamView update(@PathVariable Long id,@Valid @RequestBody ExamRequest r){return exams.save(id,r);}
 @PostMapping("/{id}/publish")public ExamView publish(@PathVariable Long id,@Valid @RequestBody VersionRequest r){return exams.publish(id,r);}
 @PostMapping("/{id}/cancel")public ExamView cancel(@PathVariable Long id,@Valid @RequestBody CancelRequest r){return exams.cancel(id,r);}
 @GetMapping("/{id}/questions")public List<StaffQuestion> questions(@PathVariable Long id){return exams.questions(id);}
 @PostMapping("/{id}/questions")public ResponseEntity<StaffQuestion> question(@PathVariable Long id,@Valid @RequestBody QuestionRequest r){return ResponseEntity.status(201).body(exams.saveQuestion(id,null,r));}
 @PutMapping("/{id}/questions/order")public List<StaffQuestion> order(@PathVariable Long id,@Valid @RequestBody OrderRequest r){return exams.reorder(id,r);}
 @PutMapping("/{id}/questions/{questionId}")public StaffQuestion editQuestion(@PathVariable Long id,@PathVariable Long questionId,@Valid @RequestBody QuestionRequest r){return exams.saveQuestion(id,questionId,r);}
 @DeleteMapping("/{id}/questions/{questionId}")public ResponseEntity<Void> remove(@PathVariable Long id,@PathVariable Long questionId,@RequestParam Long version){exams.deleteQuestion(id,questionId,version);return ResponseEntity.noContent().build();}
 @PostMapping("/{id}/attempts/start")public AttemptView start(@PathVariable Long id){return attempts.start(id);}
 @GetMapping("/attempts/{attemptId}")public AttemptView attempt(@PathVariable Long attemptId){return attempts.get(attemptId);}
 @PutMapping("/attempts/{attemptId}/answers")public ResponseEntity<AttemptView> answers(@PathVariable Long attemptId,@Valid @RequestBody SaveAnswers r){var response=attempts.save(attemptId,r);return ResponseEntity.status(response.answersAccepted()?200:409).body(response);}
 @PostMapping("/attempts/{attemptId}/submit")public AttemptView submit(@PathVariable Long attemptId){return attempts.submit(attemptId);}
 @PutMapping("/{id}/offline-results")public Gradebook offline(@PathVariable Long id,@Valid @RequestBody OfflineRequest r){return results.saveOffline(id,r);}
 @GetMapping("/{id}/offline-results")public Gradebook gradebook(@PathVariable Long id){return results.gradebook(id);}
 @PostMapping("/{id}/results/publish")public ExamView publishResults(@PathVariable Long id,@Valid @RequestBody VersionRequest r){return results.publish(id,r);}
 @GetMapping("/{id}/results")public Results results(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return results.examResults(id,page,size);}
 @GetMapping("/{id}/leaderboard")public PageResponse<RankedResult> leaderboard(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return results.leaderboard(id,page,size);}
 @GetMapping("/{id}/result")public OwnResult ownResult(@PathVariable Long id){return results.ownResult(id);}
 @GetMapping("/me/results") @PreAuthorize("hasRole('STUDENT')")public PageResponse<ResultView> mineResults(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return results.studentResults(null,page,size);}
 @GetMapping("/results/students/{studentId}")public PageResponse<ResultView> studentResults(@PathVariable Long studentId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return results.studentResults(studentId,page,size);}
 @PostMapping("/{id}/results/{resultId}/correct")public ResultView correct(@PathVariable Long id,@PathVariable Long resultId,@Valid @RequestBody CorrectionRequest r){return results.correct(id,resultId,r);}
 @GetMapping("/{id}/results/{resultId}/corrections")public List<CorrectionView> audit(@PathVariable Long id,@PathVariable Long resultId){return results.audit(id,resultId);}
}
