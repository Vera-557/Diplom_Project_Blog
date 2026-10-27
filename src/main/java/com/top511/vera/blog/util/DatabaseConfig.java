package com.top511.vera.blog.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {

    private static final String DATABASE_PROPERTIES_FILE = "db.properties";
    private static final HikariDataSource dataSource;

    static {
        try {
            registerJdbcDriver();
            Properties databaseProperties = loadDatabaseProperties();
            HikariConfig hikariConfig = getHikariConfig(databaseProperties);
            dataSource = new HikariDataSource(hikariConfig);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить конфигурацию БД", e);
        }
    }

    private DatabaseConfig() {}

    public static DataSource getDataSource() {
        return dataSource;
    }

    private static void registerJdbcDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Драйвер PostgreSQL не найден в classpath", e);
        }
    }

    private static Properties loadDatabaseProperties() throws IOException {
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(DATABASE_PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Файл db.properties не найден в classpath");
            }
            Properties props = new Properties();
            props.load(input);
            return props;
        }
    }

    private static HikariConfig getHikariConfig(Properties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));

        int maxPoolSize = 10;
        try {
            maxPoolSize = Integer.parseInt(
                    props.getProperty("db.pool.maximumPoolSize", "10"));
        } catch (NumberFormatException e) {
            System.err.println(
                    "Некорректное значение db.pool.maximumPoolSize: '"
                            + props.getProperty("db.pool.maximumPoolSize")
                            + "'. Используется значение по умолчанию: " + maxPoolSize);
        }
        config.setMaximumPoolSize(maxPoolSize);

        int minIdle = 5;
        try {
            minIdle = Integer.parseInt(
                    props.getProperty("db.pool.minimumIdle", "5"));
        } catch (NumberFormatException e) {
            System.err.println(
                    "Некорректное значение db.pool.minimumIdle: '"
                            + props.getProperty("db.pool.minimumIdle")
                            + "'. Используется значение по умолчанию: " + minIdle);
        }
        config.setMinimumIdle(minIdle);

        long connectionTimeout = 30000L;
        try {
            connectionTimeout = Long.parseLong(
                    props.getProperty("db.pool.connectionTimeout", "30000"));
        } catch (NumberFormatException e) {
            System.err.println(
                    "Некорректное значение db.pool.connectionTimeout: '"
                            + props.getProperty("db.pool.connectionTimeout")
                            + "'. Используется значение по умолчанию: " + connectionTimeout);
        }
        config.setConnectionTimeout(connectionTimeout);

        return config;
    }
}