package com.smartrent.util;

import org.flywaydb.core.Flyway;

/**
 * Utility to repair Flyway schema history.
 * Run this as a standalone Java application when migration fails.
 */
public class FlywayRepairUtil {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/smartrent";
        String user = "root";
        String password = "vuhuydiet";

        System.out.println("Starting Flyway repair...");

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();

        flyway.repair();

        System.out.println("Flyway repair completed successfully!");
        System.out.println("You can now run the application again.");
    }
}
