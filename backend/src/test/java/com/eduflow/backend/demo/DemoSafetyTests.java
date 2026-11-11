package com.eduflow.backend.demo;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoSafetyTests {
    MockEnvironment configured(){
        var env=new MockEnvironment();env.setActiveProfiles("demo");
        env.withProperty("eduflow.demo.seed.enabled","true").withProperty("eduflow.demo.seed.confirm",DemoSeedGuard.CONFIRM)
            .withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1:13306/eduflow_demo_unit")
            .withProperty("spring.jpa.hibernate.ddl-auto","validate").withProperty("server.address","127.0.0.1")
            .withProperty("app.notifications.scheduler-enabled","false").withProperty("app.exams.expiry-enabled","false");
        for(String role:List.of("ADMIN","TEACHER","STUDENT","PARENT"))env.withProperty("DEMO_"+role+"_PASSWORD","TestOnly21!"+UUID.randomUUID());
        return env;
    }
    @Test void disabledByDefaultDoesNotRequireAnyDatabaseOrPasswords(){assertThat(DemoSeedGuard.validateSettings(new MockEnvironment())).isFalse();}
    @Test void runnerIsAbsentDuringOrdinaryStartup(){new ApplicationContextRunner().withUserConfiguration(DemoDataRunner.class).run(context->assertThat(context).doesNotHaveBean(DemoDataRunner.class));}
    @Test void runnerIsAbsentEvenInDemoProfileWithoutSeedFlag(){new ApplicationContextRunner().withInitializer(c->c.getEnvironment().setActiveProfiles("demo")).withUserConfiguration(DemoDataRunner.class).run(context->assertThat(context).doesNotHaveBean(DemoDataRunner.class));}
    @Test void productionCombinationIsRefusedBeforeDatasourceInitialization(){for(String production:List.of("prod","production")){var env=configured();env.setActiveProfiles("demo",production);assertThatThrownBy(()->DemoSeedGuard.validateSettings(env)).hasMessageContaining("forbidden");}}
    @Test void flagAloneIsInsufficient(){var env=configured();env.setActiveProfiles("local");assertThatThrownBy(()->DemoSeedGuard.validateSettings(env)).hasMessageContaining("demo profile");}
    @Test void confirmationIsMandatory(){var env=configured().withProperty("eduflow.demo.seed.confirm","");assertThatThrownBy(()->DemoSeedGuard.validateSettings(env)).hasMessageContaining("confirmation");}
    @Test void realOrRemoteDatabaseNamesAreRefused(){for(String url:List.of("jdbc:mysql://127.0.0.1:3306/eduflow","jdbc:mysql://db.example:3306/eduflow_demo_seed","jdbc:mysql://127.0.0.1:3306/eduflow_test_suite")){var env=configured().withProperty("spring.datasource.url",url);assertThatThrownBy(()->DemoSeedGuard.validateSettings(env)).hasMessageContaining("isolated");}}
    @Test void schemaUpdateAndPublicBindAreRefused(){var update=configured().withProperty("spring.jpa.hibernate.ddl-auto","update");assertThatThrownBy(()->DemoSeedGuard.validateSettings(update)).hasMessageContaining("validate");var publicEnv=configured().withProperty("server.address","0.0.0.0");assertThatThrownBy(()->DemoSeedGuard.validateSettings(publicEnv)).hasMessageContaining("loopback");}
    @Test void weakPasswordsAndBackgroundReplayAreRefused(){var weak=configured().withProperty("DEMO_PARENT_PASSWORD","password");assertThatThrownBy(()->DemoSeedGuard.validateSettings(weak)).hasMessageContaining("PASSWORD");var scheduler=configured().withProperty("app.notifications.scheduler-enabled","true");assertThatThrownBy(()->DemoSeedGuard.validateSettings(scheduler)).hasMessageContaining("schedulers");}
    @Test void validExplicitDemoSettingsPass(){assertThat(DemoSeedGuard.validateSettings(configured())).isTrue();}
    @Test void historicalClockAlwaysCanReturnToLiveTime(){var clock=new DemoTimeline();var old=Instant.parse("2026-06-01T00:00:00Z");clock.at(old);assertThat(clock.instant()).isEqualTo(old);clock.live();assertThat(Duration.between(clock.instant(),Instant.now()).abs()).isLessThan(Duration.ofSeconds(2));}
    @Test void conflictingCompletionMarkersAreRefused()throws Exception{var connection=mock(Connection.class);var statement=mock(Statement.class);var result=mock(ResultSet.class);when(connection.createStatement()).thenReturn(statement);when(statement.executeQuery(anyString())).thenReturn(result);when(result.next()).thenReturn(true);when(result.getLong(1)).thenReturn(2L);assertThatThrownBy(()->DemoSeedGuard.inspect(connection)).hasMessageContaining("Conflicting");}
    @Test void mixedAccountNamespaceIsRefusedOnRerun()throws Exception{
        var connection=mock(Connection.class);var statement=mock(Statement.class);when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenAnswer(call->{var r=mock(ResultSet.class);when(r.next()).thenReturn(true);when(r.getLong(1)).thenReturn(1L);return r;});
        assertThatThrownBy(()->DemoSeedGuard.inspect(connection)).hasMessageContaining("mixed or unknown");
    }
    @Test void generatedDocumentsAreOriginalValidPdfFiles()throws Exception{
        var bytes=DemoPdf.create("Physics Formula Sheet",1);
        assertThat(new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        try(var doc=org.apache.pdfbox.Loader.loadPDF(bytes)){assertThat(doc.getNumberOfPages()).isEqualTo(1);assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains("FICTIONAL DEMONSTRATION","40 m","not sourced");}
    }
}
