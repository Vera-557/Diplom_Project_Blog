package com.top511.vera.blog.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private static final Path uploadDir;

    static {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            if (input == null) {
                throw new IllegalStateException("Файл app.properties не найден в classpath");
            }
            Properties props = new Properties();
            props.load(input);

            String raw = props.getProperty("upload.dir");
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException(
                        "Параметр upload.dir не задан в app.properties");
            }
            uploadDir = Path.of(raw).toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить конфигурацию приложения", e);
        }
    }

    private AppConfig() {}

    public static Path getUploadDir() {
        return uploadDir;
    }
}