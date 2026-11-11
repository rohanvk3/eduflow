package com.eduflow.backend;

import com.eduflow.backend.config.ProductionConfiguration;
import com.eduflow.backend.controller.HealthController;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionReadinessTests {
    @Test void databaseFailureReturns503WithoutConnectionDetails() throws Exception {
        var ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("sensitive connection details"));
        var response = new HealthController(ds).readiness();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("status", "DOWN").hasSize(1);
    }
    @Test void readinessClosesConnection() throws Exception {
        var ds = mock(DataSource.class); var connection = mock(Connection.class);
        when(ds.getConnection()).thenReturn(connection); when(connection.isValid(2)).thenReturn(true);
        assertThat(new HealthController(ds).readiness().getStatusCode().value()).isEqualTo(200);
        verify(connection).close();
    }
    @Test void productionRejectsMissingOrBlankConfiguration() {
        assertThatThrownBy(() -> new ProductionConfiguration(new MockEnvironment())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionConfiguration(new MockEnvironment().withProperty("DB_URL", " "))).hasMessageContaining("DB_URL");
    }
    @Test void testDatabaseGuardRejectsApplicationDatabaseBeforeStartup() {
        var context = new org.springframework.context.support.GenericApplicationContext();
        context.setEnvironment(new MockEnvironment().withProperty("EDUFLOW_TEST_DB_URL", "jdbc:mysql://localhost/eduflow"));
        assertThatThrownBy(() -> new IsolatedMySqlInitializer().initialize(context)).hasMessageContaining("dedicated");
        context.close();
    }
}
