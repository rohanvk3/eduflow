package com.eduflow.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NotificationClockConfig {
    @Bean("notificationClock")
    public Clock notificationClock(org.springframework.beans.factory.ObjectProvider<com.eduflow.backend.demo.DemoTimeline> demo) {
        Clock clock=demo.getIfAvailable();return clock==null?Clock.systemUTC():clock;
    }

    @Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("eduflow-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }
}
