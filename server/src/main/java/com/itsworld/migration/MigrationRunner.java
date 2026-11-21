package com.itsworld.migration;

import org.flywaydb.core.Flyway;

/** 发布前使用同一个应用包及凭据迁移，不启动 Web 应用。 */
public final class MigrationRunner {
    private MigrationRunner() { }

    public static void main(String[] args) {
        String command = args.length == 1 ? args[0] : "";
        if (!java.util.Set.of("migrate", "validate", "info", "baseline-v18").contains(command)) {
            throw new IllegalArgumentException("仅支持 migrate / validate / info / baseline-v18");
        }
        var flyway = Flyway.configure()
                .dataSource(required("SPRING_DATASOURCE_URL"), required("SPRING_DATASOURCE_USERNAME"),
                        required("SPRING_DATASOURCE_PASSWORD"))
                .locations("classpath:db/migration")
                .baselineOnMigrate(false).baselineVersion("18")
                .baselineDescription("已核验的既有生产结构 V18")
                .cleanDisabled(true).validateOnMigrate(true).load();
        switch (command) {
            case "migrate" -> flyway.migrate();
            case "validate" -> flyway.validate();
            case "info" -> {
                for (var migration : flyway.info().all()) {
                    System.out.println(migration.getVersion() + " " + migration.getState());
                }
            }
            case "baseline-v18" -> {
                // 防止把未核对的库直接标记为已经执行过历史迁移。
                if (!"18".equals(System.getenv("FLYWAY_VERIFIED_BASELINE"))) {
                    throw new IllegalStateException("先核验 V18 结构并备份，再设置 FLYWAY_VERIFIED_BASELINE=18");
                }
                flyway.baseline();
                flyway.validate();
            }
            default -> throw new IllegalArgumentException(command);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("缺少环境变量 " + name);
        return value;
    }
}
