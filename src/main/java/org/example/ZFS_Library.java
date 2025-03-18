package org.example;

import java.io.*;
import java.nio.file.*;
import java.security.*;

public class ZFS_Library {
    private static final String POOL_NAME = "mypool"; // Name des ZFS-Pools

    public static String checkIn(String filePath) throws IOException {
        String hash = getFileHash(filePath); // Berechnet den aktuellen Hash der Datei
        System.out.println("Check-In abgeschlossen. Aktueller Hash: " + hash);
        return hash; // Gibt den Hash zurück, um ihn für spätere Checks zu speichern
    }


    /**
     * Erstellt einen ZFS-Snapshot beim Commit der Transaktion.
     */
    public static void commitTransaction(String snapshotName) throws IOException {
        executeCommand("sudo zfs snapshot " + snapshotName);
        System.out.println("Snapshot erstellt: " + snapshotName);
    }

    /**
     * Setzt das Dateisystem auf den letzten konsistenten Zustand zurück.
     */
    public static void rollbackTransaction(String snapshotName) throws IOException {
        executeCommand("sudo zfs rollback " + snapshotName);
        System.out.println("Rollback durchgeführt: " + snapshotName);
    }

    /**
     * Gibt den Pfad einer temporären Datei basierend auf einem Originalpfad und einem Zeitstempel zurück.
     */
    public static String createTempFilePath(String originalFilePath) {
        return originalFilePath + "_" + System.currentTimeMillis() + ".tmp";
    }

    /**
     * Bearbeitet den Inhalt einer Datei mit temporärer Kopie und integriertem Konfliktmanagement.
     * Der neue Inhalt wird komplett in der temporären Datei geschrieben.
     */
    public static boolean overwriteFileWithTemp(String originalFilePath, String newContent) throws IOException {
        String tempFilePath = createTempFilePath(originalFilePath);

        // Temporäre Kopie der Originaldatei erstellen
        Files.copy(Paths.get(originalFilePath), Paths.get(tempFilePath), StandardCopyOption.REPLACE_EXISTING);

        // Originaldatei-Hash speichern
        String originalHash = getFileHash(originalFilePath);

        // Schreiben des neuen Inhalts in die temporäre Datei
        Files.writeString(Paths.get(tempFilePath), newContent);
        System.out.println("Temporäre Datei mit neuem Inhalt überschrieben: " + tempFilePath);

        // Nach Bearbeitung Hash der Originaldatei prüfen
        if (hasFileChanged(originalFilePath, originalHash)) {
            // Konflikt erkannt – temporäre Datei löschen
            System.out.println("⚠️ Konflikt erkannt! Datei wurde während der Bearbeitung verändert.");
            Files.deleteIfExists(Paths.get(tempFilePath));
            return false; // Änderungen NICHT übernommen
        } else {
            // Keine Konflikte – temporäre Datei in Originaldatei übernehmen
            Files.move(Paths.get(tempFilePath), Paths.get(originalFilePath), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Änderungen übernommen und in Originaldatei gespeichert.");
            return true; // Änderungen übernommen
        }
    }

    /**
     * Schreibt den angegebenen Inhalt in eine Datei und setzt korrekte Berechtigungen.
     */
    public static void writeFile(String filePath, String content) throws IOException {
        executeCommand("echo '" + content + "' | sudo tee " + filePath + " > /dev/null");
        executeCommand("sudo chmod 666 " + filePath); // Setzt Lese- und Schreibrechte für alle Benutzer
        System.out.println("Datei geschrieben: " + filePath);
    }

    /**
     * Liest den Inhalt einer Datei und gibt ihn als String zurück.
     */
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    /**
     * Löscht eine Datei.
     */
    public static void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
        System.out.println("Datei gelöscht: " + filePath);
    }

    /**
     * Berechnet den SHA-256-Hash einer Datei zur Konfliktprüfung.
     */
    public static String getFileHash(String filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }
            return hashString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithmus nicht verfügbar!", e);
        }
    }

    /**
     * Prüft, ob sich der Hashwert einer Datei seit der letzten Transaktion geändert hat.
     */
    public static boolean hasFileChanged(String filePath, String oldHash) throws IOException {
        return !getFileHash(filePath).equals(oldHash);
    }

    /**
     * Führt einen beliebigen Shell-Befehl aus.
     */
    private static void executeCommand(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.inheritIO(); // Terminal-Eingabe und -Ausgabe vererben
        Process process = processBuilder.start();
        try {
            process.waitFor(); // Warten, bis der Prozess beendet ist
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
