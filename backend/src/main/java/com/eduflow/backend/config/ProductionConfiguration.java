package com.eduflow.backend.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Reject blank production values before a usable application can start. */
@Configuration
@Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(Environment env) {
        for (String key : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "FRONTEND_ORIGINS", "FILE_STORAGE_DIR"}) {
            if (env.getRequiredProperty(key).isBlank()) throw new IllegalStateException("Required production configuration is blank: " + key);
        }
        if (!Path.of(env.getRequiredProperty("FILE_STORAGE_DIR")).isAbsolute())
            throw new IllegalStateException("FILE_STORAGE_DIR must be an absolute persistent path");
        if (!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))
            throw new IllegalStateException("Production requires schema validation; apply reviewed migrations separately");
    }
}
