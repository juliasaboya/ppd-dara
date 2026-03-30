#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"
MANIFEST_FILE="$BUILD_DIR/MANIFEST.MF"
JAR_NAME="dara-game.jar"

rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR/lib" "$CLASSES_DIR/dara/ui"

javac -cp "$ROOT_DIR/lib/jsvg-2.0.0.jar" -d "$CLASSES_DIR" $(find "$ROOT_DIR/src" -name '*.java' | sort)
cp -R "$ROOT_DIR/src/dara/ui/images" "$CLASSES_DIR/dara/ui/"
cp "$ROOT_DIR/lib/jsvg-2.0.0.jar" "$DIST_DIR/lib/"

cat > "$MANIFEST_FILE" <<EOF
Main-Class: Main
Class-Path: lib/jsvg-2.0.0.jar

EOF

jar cfm "$DIST_DIR/$JAR_NAME" "$MANIFEST_FILE" -C "$CLASSES_DIR" .

echo "Jar gerado em: $DIST_DIR/$JAR_NAME"
echo "Execute com:"
echo "  cd $DIST_DIR"
echo "  java -jar $JAR_NAME"
