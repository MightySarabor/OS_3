#!/bin/bash

# Maven Build ausführen
mvn clean package
if [ $? -ne 0 ]; then
    echo "Build fehlgeschlagen. Skript wird beendet."
    exit 1
fi

# Zielverzeichnis definieren
TARGET_DIR="/home/USERNAME/Desktop/VirtualBox"

# Sicherstellen, dass das Zielverzeichnis existiert
if [ ! -d "$TARGET_DIR" ]; then
    echo "Zielverzeichnis existiert nicht. Es wird erstellt."
    mkdir -p "$TARGET_DIR"
fi

# Dateien aus dem target-Verzeichnis kopieren und vorhandene überschreiben
cp -r target/* "$TARGET_DIR"

echo "Dateien erfolgreich kopiert!"
