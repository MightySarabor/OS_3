package org.example;

import java.io.*;
import java.util.Scanner;

public class BrainstormingTool {
    private static final String IDEEN_VERZEICHNIS = "/mypool/ideen/";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new File(IDEEN_VERZEICHNIS).mkdirs(); // Sicherstellen, dass das Verzeichnis existiert

        while (true) {
            System.out.println("\nBrainstorming-Tool - Optionen:");
            System.out.println("1. Neue Idee hinzufügen");
            System.out.println("2. Ideen auflisten");
            System.out.println("3. Idee anzeigen");
            System.out.println("4. Idee kommentieren");
            System.out.println("5. Beenden");
            System.out.print("> ");

            int auswahl = scanner.nextInt();
            scanner.nextLine(); // Zeilenumbruch verarbeiten

            try {
                switch (auswahl) {
                    case 1 -> neueIdee();
                    case 2 -> ideenAuflisten();
                    case 3 -> ideeAnzeigen();
                    case 4 -> ideeKommentieren();
                    case 5 -> {
                        System.out.println("Beenden...");
                        return;
                    }
                    default -> System.out.println("Ungültige Auswahl!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void neueIdee() throws IOException {
        System.out.print("Titel der Idee (keine Leerzeichen erlaubt): ");
        String titel = scanner.nextLine().replaceAll("\\s+", "_"); // Leerzeichen durch Unterstrich ersetzen
        String dateipfad = IDEEN_VERZEICHNIS + titel + ".txt";

        if (new File(dateipfad).exists()) {
            System.out.println("⚠️ Die Idee existiert bereits. Möchtest du sie kommentieren? (ja/nein)");
            String antwort = scanner.nextLine().trim().toLowerCase();

            if (antwort.equals("ja")) {
                ideeKommentieren2(titel); // Direkt ins Kommentieren wechseln
            } else {
                System.out.println("Erstellung abgebrochen.");
            }
            return;
        }

        System.out.print("Beschreibung der Idee: ");
        String beschreibung = scanner.nextLine();
        ZFS_Library.writeFile(dateipfad, beschreibung);
    }

    private static void ideenAuflisten() {
        File ordner = new File(IDEEN_VERZEICHNIS);
        File[] dateien = ordner.listFiles();

        if (dateien == null || dateien.length == 0) {
            System.out.println("Keine Ideen vorhanden.");
            return;
        }

        System.out.println("Gespeicherte Ideen:");
        for (File datei : dateien) {
            System.out.println("- " + datei.getName().replace(".txt", ""));
        }
    }

    private static void ideeAnzeigen() throws IOException {
        System.out.print("Titel der Idee: ");
        String titel = scanner.nextLine().replaceAll("\\s+", "_");
        String dateipfad = IDEEN_VERZEICHNIS + titel + ".txt";

        String inhalt = ZFS_Library.readFile(dateipfad);
        System.out.println("\nInhalt der Idee:");
        System.out.println(inhalt);
    }

    private static void ideeKommentieren() throws IOException {
        System.out.print("Titel der Idee: ");
        String titel = scanner.nextLine().replaceAll("\\s+", "_");
        ideeKommentieren2(titel);
    }

    private static void ideeKommentieren2(String titel) throws IOException {
        String dateipfad = IDEEN_VERZEICHNIS + titel + ".txt";
        System.out.print("Neuer Kommentar (vorherigen Inhalt überschreiben): ");

        ZFS_Library.overwriteFile(dateipfad);
    }
}
