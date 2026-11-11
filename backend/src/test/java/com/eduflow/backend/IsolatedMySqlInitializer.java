package com.eduflow.backend;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Run before datasource creation, including before Hibernate schema updates. */
public class IsolatedMySqlInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getRequiredProperty("EDUFLOW_TEST_DB_URL");
        if (!url.matches("jdbc:mysql://[^/]+/eduflow_test_[a-zA-Z0-9_]+(\\?.*)?"))
            throw new IllegalStateException("MySQL tests require a dedicated eduflow_test_* database and restricted test credentials");
    }
}
