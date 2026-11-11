package com.eduflow.backend.config;
import java.time.Clock;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class AssignmentConfig {
 @Bean("assignmentClock") public Clock assignmentClock(org.springframework.beans.factory.ObjectProvider<com.eduflow.backend.demo.DemoTimeline> demo){Clock clock=demo.getIfAvailable();return clock==null?Clock.systemUTC():clock;}
 @Bean public MultipartConfigElement multipartConfigElement(@Value("${app.assignments.max-file-bytes:10485760}")long limit){
  if(limit<1024||limit>52428800)throw new IllegalArgumentException("Assignment file limit must be 1 KiB to 50 MiB");
  return new MultipartConfigElement("",limit,Math.addExact(Math.multiplyExact(limit,10),262144),0);
 }
}
