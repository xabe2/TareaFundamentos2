#!/bin/bash
# ============================================================
#  run.sh — Ejecutar el Reproductor MP3 + LRC
#  Uso: ./run.sh [archivo.mp3] [archivo.lrc]
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIBS="libs/*"
JAVA="java"

if ! command -v java &>/dev/null; then
    for candidate in \
        "/usr/lib/jvm/java-21-openjdk-amd64/bin/java" \
        "/usr/lib/jvm/java-17-openjdk-amd64/bin/java"; do
        if [ -x "$candidate" ]; then
            JAVA="$candidate"
            break
        fi
    done
fi

MP3="${1:-}"
LRC="${2:-}"

$JAVA -cp ".:$LIBS" Reproductor $MP3 $LRC
