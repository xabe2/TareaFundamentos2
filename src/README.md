# Reproductor MP3 + LRC — Tarea 2 FCC 2026

## Estructura del proyecto

```
repmp3/
├── compile.sh                  ← Script de compilación
├── run.sh                      ← Script de ejecución
├── Reproductor.java            ← Reproductor principal (Etapa 3)
├── lyrics/
│   ├── lrc.sablecc             ← Gramática LR para SableCC (Etapa 1)
│   ├── LyricItem.java          ← Modelo de datos: un ítem de lyric
│   └── LrcParser.java          ← Parser manual del formato .lrc (Etapa 1+2)
├── libs/                       ← JARs de BasicPlayer
│   ├── basicplayer3.0.jar
│   ├── mp3spi1.9.4.jar
│   └── ...
├── metallica.mp3
├── nothing_else_matters.lrc
├── evanescense.mp3
└── bring_me_to_life.lrc
```

---

## Compilar

```bash
chmod +x compile.sh run.sh
./compile.sh
```

## Ejecutar

```bash
# Con argumentos de línea de comandos:
./run.sh metallica.mp3 nothing_else_matters.lrc
./run.sh evanescense.mp3 bring_me_to_life.lrc

# Sin argumentos (usar botones de la GUI):
./run.sh
```

---

## Las 3 etapas implementadas

### Etapa 1 — Gramática LR (`lyrics/lrc.sablecc`)

Gramática SableCC para reconocer archivos `.lrc`:

```
archivo → metadata lineas | lineas
metadata → (meta_tag eol)+
meta_tag → tag_ar | tag_ti | tag_al | tag_length | tag_other
lineas   → linea+
linea    → time_tag lyric_text? eol
time_tag → '[' dd ':' dd '.' dd ']'
```

El árbol de derivación se muestra en el panel inferior de la GUI
al cargar cualquier archivo `.lrc`.

Para regenerar el parser con SableCC:
```bash
java -jar sablecc.jar lyrics/lrc.sablecc
# Genera: lyrics/lexer/, lyrics/parser/, lyrics/node/, lyrics/analysis/
```

### Etapa 2 — Visitador (`lyrics/LrcParser.java`)

Implementa el recorrido del árbol y almacena cada ítem en un
`ArrayList<LyricItem>` (repositorio), donde cada `LyricItem`
tiene:
- `timeMs` — tiempo en milisegundos
- `text`   — texto del lyric

### Etapa 3 — Reproductor + Timer (`Reproductor.java`)

Usa la clase interna `Reminder` (basada en `java.util.Timer`) para
programar la aparición de cada línea exactamente en su timestamp.
La GUI muestra la línea actual en verde grande y la siguiente en
gris tenue.

---

## Metadatos soportados

| Tag        | Significado        |
|------------|--------------------|
| `[ar:...]` | Artista            |
| `[ti:...]` | Título de la canción|
| `[al:...]` | Álbum              |
| `[length:]`| Duración           |
| `[xx:...]` | Otros (ignorados)  |

---

## Requisitos

- Java 11 o superior (JDK para compilar, JRE para ejecutar)
- Los JARs de BasicPlayer incluidos en `libs/`
- Para Etapa 1 completa con SableCC: descargar `sablecc.jar` de https://sablecc.org
