package org.example;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestZFSLibrary {

    private static final String BASE_DIRECTORY = "/mypool/";
    private static final String[] FILE_NAMES = {"Testfile_1.txt", "Testfile_2.txt", "Testfile_3.txt"};
    private static final Random RANDOM = new Random();

    // Statistische Zähler
    private static final AtomicInteger totalOperations = new AtomicInteger(0);
    private static final AtomicInteger conflicts = new AtomicInteger(0);
    private static final AtomicInteger rollbacks = new AtomicInteger(0);
    private static final AtomicInteger snapshotsCreated = new AtomicInteger(0);
    private static final AtomicInteger readOperations = new AtomicInteger(0);
    private static final AtomicInteger overwriteOperations = new AtomicInteger(0);
    private static final AtomicInteger newFileOperations = new AtomicInteger(0);

    private static void randomFileOperation(String threadName) {
        int randomValue = RANDOM.nextInt(100);
        String fileName = FILE_NAMES[RANDOM.nextInt(FILE_NAMES.length)];
        String filePath = BASE_DIRECTORY + fileName;

        try {
            if (randomValue < 40) {
                // 40% Wahrscheinlichkeit: Datei lesen
                ZFS_Library.readFile(filePath);
                readOperations.incrementAndGet();
            } else if (randomValue < 95) {
                // 55% Wahrscheinlichkeit: Datei überschreiben
                String orignalHash = ZFS_Library.checkIn(filePath);
                Thread.sleep(200);
                String newHash = ZFS_Library.checkIn(filePath);
                ZFS_Library.overwriteFile(filePath, "Inhalt bearbeitet von " + threadName);
                overwriteOperations.incrementAndGet();
                if (!orignalHash.equals(newHash))
                    conflicts.incrementAndGet();
                else
                    snapshotsCreated.incrementAndGet();
                ZFS_Library.checkout(orignalHash, newHash);
            } else {
                // 5% Wahrscheinlichkeit: Neue Datei erstellen
                ZFS_Library.writeFile(filePath, "Neue Datei erstellt von " + threadName);
                newFileOperations.incrementAndGet();
                snapshotsCreated.incrementAndGet();
            }
            totalOperations.incrementAndGet();
        } catch (IOException e) {
            if (e.getMessage().contains("Konflikt erkannt")) {
                conflicts.incrementAndGet();
                rollbacks.incrementAndGet();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // Anzahl der Threads
        int threadCount = 10;
        int operationCountPerThread = 50; // Anzahl der Operationen pro Thread

        // Timer starten
        long startTime = System.currentTimeMillis();

        // ExecutorService für Thread-Pool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            String threadName = "Thread-" + i;

            executor.submit(() -> {
                for (int j = 0; j < operationCountPerThread; j++) {
                    randomFileOperation(threadName);

                    // Zufällige Pause, um Parallelität zu simulieren
                    try {
                        Thread.sleep(RANDOM.nextInt(200)); // Pause zwischen 0 und 200ms
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // ExecutorService beenden und warten, bis alle Threads abgeschlossen sind
        executor.shutdown();
        while (!executor.isTerminated()) {
            // Warten, bis alle Tasks abgeschlossen sind
        }

        // Timer stoppen
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n==== Auswertung ====");
        System.out.println("Dateien gelesen: " + readOperations.get());
        System.out.println("Dateien überschrieben: " + overwriteOperations.get());
        System.out.println("Neue Dateien erstellt: " + newFileOperations.get());
        System.out.println("Gesamtoperationen: " + totalOperations.get());
        System.out.println("Dauer der Ausführung: " + duration + " ms");
        System.out.println("Konflikte erkannt: " + conflicts.get());
        System.out.println("Snapshots erstellt: " + snapshotsCreated.get());
    }
}
