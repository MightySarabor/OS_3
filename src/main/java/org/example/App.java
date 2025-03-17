package org.example;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        ZFS_Library tx = new ZFS_Library();
        String testFile = "/mypool/testfile.txt";

        try {
            // Transaktion starten (Snapshot erstellen)
            tx.beginTransaction();

            // Datei schreiben
            tx.writeFile(testFile, "Hallo ZFS! Testdaten.");

            // Datei lesen und ausgeben
            String content = tx.readFile(testFile);
            System.out.println("Dateiinhalt: " + content);

            // Simulierte Bedingung: Falls Konflikt erkannt wird -> Rollback
            boolean conflictDetected = false; // Hier kann später eine echte Prüfung erfolgen

            if (conflictDetected) {
                System.out.println("Konflikt erkannt, Rollback wird durchgeführt...");
                tx.rollbackTransaction();
            } else {
                System.out.println("Kein Konflikt, Commit wird durchgeführt...");
                //tx.commitTransaction();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
