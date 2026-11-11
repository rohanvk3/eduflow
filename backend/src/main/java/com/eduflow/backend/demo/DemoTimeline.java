package com.eduflow.backend.demo;

import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Only present during explicitly enabled demo replay. Returns to real time in finally. */
@Component @org.springframework.context.annotation.Primary @Profile("demo")
@ConditionalOnProperty(name="eduflow.demo.seed.enabled",havingValue="true")
public class DemoTimeline extends Clock {
    private volatile Instant replay;
    public void at(Instant instant){replay=instant;}
    public void live(){replay=null;}
    public ZoneId getZone(){return ZoneOffset.UTC;}
    public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
    public Instant instant(){var value=replay;return value==null?Instant.now():value;}
}
