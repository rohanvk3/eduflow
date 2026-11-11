package com.eduflow.backend.service;
import com.eduflow.backend.repository.ExamAttemptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.exams.expiry-enabled",havingValue="true",matchIfMissing=true)
public class ExamExpiryJob {
 final ExamAttemptRepository attempts;final ExamAttemptService service;final ExamAccess access;
 public ExamExpiryJob(ExamAttemptRepository attempts,ExamAttemptService service,ExamAccess access){this.attempts=attempts;this.service=service;this.access=access;}
 @Scheduled(fixedDelayString="${app.exams.expiry-delay-ms:15000}")
 public void expire(){
  for(var id:attempts.expired(access.now(),PageRequest.of(0,200))){
   try{service.expire(id);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Exam attempt finalization will be retried: {}",id,ex);}
  }
 }
}
