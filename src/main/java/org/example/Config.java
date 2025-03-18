package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis); // Lade die Konfigurationsdatei
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Konfigurationsdatei: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue); // Rückgabe des Werts oder Standard
    }
}
