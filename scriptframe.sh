#!/bin/bash

# Définition des chemins
SRC_DIR="src"
BUILD_DIR="build"
DIST_DIR="dist"
JAR_NAME="framework.jar"

# Nettoyage des anciens fichiers
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$DIST_DIR"

javac -d "$BUILD_DIR" $(find "$SRC_DIR" -name "*.java")

jar cf "$DIST_DIR/$JAR_NAME" -C "$BUILD_DIR" .

echo "Compilation terminée. Le fichier JAR est disponible dans $DIST_DIR/$JAR_NAME"

sleep 60