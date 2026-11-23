package com.itsworld.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlywayMigrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("flyway_test").withUsername("test").withPassword("test");

    @Test
    void emptyDatabaseMigratesOnceAndExistingDatabaseRequiresExplicitBaseline() throws Exception {
        var flyway = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").baselineOnMigrate(false)
                .baselineVersion("18").cleanDisabled(true).load();
        assertEquals(2, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        try (var connection = java.sql.DriverManager.getConnection(MYSQL.getJdbcUrl(), "test", "test");
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema=DATABASE() AND table_name<>'flyway_schema_history'")) {
                rows.next();
                assertEquals(92, rows.getInt(1));
            }
            // 模拟已经存在业务表、尚未接入 Flyway 的环境。
            statement.execute("DROP TABLE assistant_page_task");
            statement.execute("DROP TABLE flyway_schema_history");
            assertThrows(FlywayException.class, flyway::migrate);
            flyway.baseline();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            assertEquals(0, flyway.migrate().migrationsExecuted);
            try (var rows = statement.executeQuery("SELECT version,type,success FROM flyway_schema_history ORDER BY installed_rank")) {
                assertTrue(rows.next());
                assertEquals("18", rows.getString(1));
                assertEquals("BASELINE", rows.getString(2));
                assertTrue(rows.getBoolean(3));
                assertTrue(rows.next());
                assertEquals("19", rows.getString(1));
                assertEquals("SQL", rows.getString(2));
                assertTrue(rows.getBoolean(3));
            }
            assertThrows(FlywayException.class, flyway::clean);
        }
    }
}
