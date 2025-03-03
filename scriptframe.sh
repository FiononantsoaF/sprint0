#!/bin/bash

# Définition des chemins
SRC_DIR="src"
BUILD_DIR="build"
DIST_DIR="dist"
JAR_NAME="framework.jar"
LIB_DIR="lib"

# Nettoyage des anciens fichiers
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$DIST_DIR"

# Compilation avec inclusion des bibliothèques dans lib
javac -cp "$LIB_DIR/*" -d "$BUILD_DIR" $(find "$SRC_DIR" -name "*.java")

# Création du JAR
jar cf "$DIST_DIR/$JAR_NAME" -C "$BUILD_DIR" .

cp "$LIB_DIR"/* "$DIST_DIR"/

echo "Compilation terminée. Le fichier JAR est disponible dans $DIST_DIR/$JAR_NAME"

sleep 60