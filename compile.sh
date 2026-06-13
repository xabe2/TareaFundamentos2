#!/bin/bash
# ============================================================
#  compile.sh — Compilar el proyecto Reproductor LRC
#  Tarea 2 - Fundamentos de Ciencias de la Computación
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIBS="libs/*"
JAVAC="javac"

# Detectar javac si no está en PATH
if ! command -v javac &>/dev/null; then
    for candidate in \
        "/usr/lib/jvm/java-21-openjdk-amd64/bin/javac" \
        "/usr/lib/jvm/java-17-openjdk-amd64/bin/javac" \
        "/usr/local/bin/javac"; do
        if [ -x "$candidate" ]; then
            JAVAC="$candidate"
            break
        fi
    done
fi

echo "==================================================="
echo "  Compilando Reproductor MP3 + LRC"
echo "==================================================="

# 1) Compilar el paquete lyrics (parser + modelo)
echo "[1/2] Compilando paquete lyrics..."
$JAVAC -cp "$LIBS" -d . lyrics/LyricItem.java lyrics/LrcParser.java

# 2) Compilar el reproductor principal
echo "[2/2] Compilando Reproductor.java..."
$JAVAC -cp ".:$LIBS" Reproductor.java

echo ""
echo "✓ Compilación exitosa."
echo ""
echo "Para ejecutar:"
echo "  ./run.sh metallica.mp3 nothing_else_matters.lrc"
echo "  ./run.sh evanescense.mp3 bring_me_to_life.lrc"
echo ""
