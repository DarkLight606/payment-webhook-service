package io.github.darklight606.paymentwebhook;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentWebhookApplicationIT {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads_applicationStarted_contextIsActive() {
        assertThat(applicationContext.isActive()).isTrue();
    }

    @Test
    void flywayInfo_afterMigration_versionOneAppliedSuccessfully() {
        var current = flyway.info().current();

        assertThat(current).isNotNull();
        assertThat(current.getVersion().toString()).isEqualTo("1");
        assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    void databaseMetadata_afterConnect_serverMajorVersionIsSixteen() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
        }
    }
}
