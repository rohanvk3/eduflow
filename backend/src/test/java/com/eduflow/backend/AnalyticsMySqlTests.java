package com.eduflow.backend;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "EDUFLOW_MYSQL_TEST", matches = "true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties = {"spring.datasource.url=${EDUFLOW_TEST_DB_URL}", "spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}", "spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}", "spring.jpa.hibernate.ddl-auto=update", "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE="})
class AnalyticsMySqlTests extends AcademicMySqlTests {
    @Test
    void realMysqlAggregationsAndExports() throws Exception {
        var u = users.saveAndFlush(new User("Analytics QA", UUID.randomUUID() + "@qa.invalid", "unused", Role.ADMIN));
        token = jwt.generateToken(u.getEmail());
        for (String path : List.of("overview", "finance/summary", "finance/outstanding", "attendance/trend", "academics/summary", "academics/assignment-performance", "batches", "teachers"))
            mvc.perform(get("/api/analytics/" + path).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/reports/export/pdf").param("reportType", "BATCH_PERFORMANCE").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
