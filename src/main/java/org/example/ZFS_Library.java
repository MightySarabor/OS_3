package org.example;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;

public class ZFS_Library {
    private static final String POOL_NAME = Config.get("zfs.pool.name", "defaultpool");
    private static final String SNAPSHOT_DIR = Config.get("zfs.snapshot.dir", "/default/snapshot");
    /**
     * Check-In: Berechnet und gibt den Hash der Datei zurück.
     */
    public static String checkIn(String filePath) throws IOException {
        String hash = getFileHash(filePath);
        System.out.println("Check-In abgeschlossen. Aktueller Hash: " + hash);
        return hash;
    }

    public static void overwriteFile(String filePath, String newContent) throws IOException {
        Files.writeString(Paths.get(filePath), newContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // System.out.println("Datei überschrieben: " + filePath);
    }

    /**
     * Check-Out: Vergleicht zwei Hashes und führt entweder einen Rollback mit dem neuesten Snapshot
     * oder ein Commit durch.
     */
    public static void checkout(String originalHash, String newHash) throws IOException, InterruptedException {
        // Vergleich der Hashes: Wenn sie unterschiedlich sind, Rollback durchführen
        if (!originalHash.equals(newHash)) {
            String latestSnapshot = getLatestSnapshot();
            if (latestSnapshot != null) {
                System.out.println("⚠️ Konflikt erkannt! Rollback auf den neuesten Snapshot: " + latestSnapshot);
                rollbackTransaction(latestSnapshot);
            } else {
                System.out.println("❌ Kein Snapshot vorhanden. Rollback kann nicht durchgeführt werden.");
            }
        } else {
            // Wenn die Hashes übereinstimmen, Commit durchführen
            String snapshotName = "snapshot_" + System.currentTimeMillis();
            commitTransaction(snapshotName);
            // System.out.println("✅ Änderungen bestätigt und Snapshot erstellt: " + snapshotName);
        }
    }

    /**
     * Liest den aktuellsten Snapshot aus dem ZFS-Dateisystem.
     */
    private static String getLatestSnapshot() throws IOException {
        String snapshotDir = "/mypool/.zfs/snapshot"; // Pfad zu den Snapshots
        File snapshotFolder = new File(snapshotDir);

        if (snapshotFolder.exists() && snapshotFolder.isDirectory()) {
            // Alle Snapshots abrufen und sortieren
            File[] snapshots = snapshotFolder.listFiles();
            if (snapshots != null && snapshots.length > 0) {
                // Nach Änderungszeitpunkt sortieren (neuester zuletzt)
                Arrays.sort(snapshots, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                return snapshots[0].getName(); // Neuester Snapshot
            }
        }
        return null; // Kein Snapshot gefunden
    }

    /**
     * Erstellt einen ZFS-Snapshot.
     */
    private static void commitTransaction(String snapshotName) throws IOException, InterruptedException {
        executeCommand("sudo zfs snapshot " + POOL_NAME + "@" + snapshotName);
        // System.out.println("Snapshot erstellt: " + snapshotName);
    }

    /**
     * Rollback zu einem ZFS-Snapshot.
     */
    private static void rollbackTransaction(String snapshotName) throws IOException, InterruptedException {
        executeCommand("sudo zfs rollback " + POOL_NAME + "@" + snapshotName);
        System.out.println("Rollback durchgeführt: " + snapshotName);
    }

    /**
     * Liest den Inhalt einer Datei und gibt ihn als String zurück.
     */
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    public static void writeFile(String filePath, String content) throws IOException, InterruptedException {
        File file = new File(filePath);

        // Datei schreiben
        Files.writeString(Paths.get(filePath), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // System.out.println("Datei erfolgreich geschrieben: " + filePath);

        String snapshotName = "snapshot_" + System.currentTimeMillis();
        commitTransaction(snapshotName);
        // System.out.println("✅ Snapshot nach Erstellen der neuen Datei erstellt: " + snapshotName);
    }

    /**
     * Berechnet den SHA-256-Hash einer Datei.
     */
    private static String getFileHash(String filePath) throws IOException {
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
     * Prüft, ob sich der Hashwert der Datei geändert hat.
     */
    private static boolean hasFileChanged(String filePath, String oldHash) throws IOException {
        return !getFileHash(filePath).equals(oldHash);
    }

    /**
     * Löscht eine Datei und erstellt einen Snapshot nach dem Löschen.
     */
    public static void deleteFile(String filePath) throws IOException, InterruptedException {
        File file = new File(filePath);

        if (file.exists()) {
            // Lösche die Datei
            if (file.delete()) {
                // System.out.println("✅ Datei erfolgreich gelöscht: " + filePath);

                // Nach dem Löschen einen Snapshot erstellen
                String snapshotName = "snapshot_" + System.currentTimeMillis();
                commitTransaction(snapshotName);
                // System.out.println("✅ Snapshot nach Löschen erstellt: " + snapshotName);
            } else {
                System.out.println("❌ Datei konnte nicht gelöscht werden: " + filePath);
            }
        } else {
            System.out.println("❌ Datei nicht gefunden: " + filePath);
        }
    }

    /**
     * Führt einen Shell-Befehl aus.
     */
    private static void executeCommand(String command) throws IOException, InterruptedException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Ausführen des Befehls: " + command);
            e.printStackTrace();
            throw e;
        }
    }

}
