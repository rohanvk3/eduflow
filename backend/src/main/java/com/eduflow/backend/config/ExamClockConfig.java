package com.eduflow.backend.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration
@EnableScheduling
public class ExamClockConfig {
 @Bean("examClock")
 public Clock examClock(org.springframework.beans.factory.ObjectProvider<com.eduflow.backend.demo.DemoTimeline> demo){Clock clock=demo.getIfAvailable();return clock==null?Clock.systemUTC():clock;}
}
