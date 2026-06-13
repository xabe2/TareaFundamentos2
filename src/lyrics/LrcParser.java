package lyrics;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.*;

/**
 * Parser manual para archivos .lrc que implementa la gramática SableCC definida.
 *
 * Gramática (resumen):
 *   archivo     → metadata lineas | lineas
 *   metadata    → (meta_tag eol)+
 *   meta_tag    → tag_ar | tag_ti | tag_al | tag_length | tag_other
 *   lineas      → linea+
 *   linea       → time_tag lyric_text? eol
 *   time_tag    → '[' dd ':' dd '.' dd ']'
 *
 * Tarea 2 - Fundamentos de Ciencias de la Computación
 */
public class LrcParser {

    // ── Patrones léxicos (tokens) ──────────────────────────────────────────
    private static final Pattern TIME_TAG   =
            Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)$");
    private static final Pattern TAG_AR     =
            Pattern.compile("^\\[ar:(.*)\\]\\s*$",   Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_TI     =
            Pattern.compile("^\\[ti:(.*)\\]\\s*$",   Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_AL     =
            Pattern.compile("^\\[al:(.*)\\]\\s*$",   Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_LENGTH =
            Pattern.compile("^\\[length:(.*)\\]\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_OTHER  =
            Pattern.compile("^\\[[a-zA-Z]{2}:.*\\]\\s*$");

    // ── Campos del árbol de derivación / repositorio ───────────────────────
    private String artist  = "";
    private String title   = "";
    private String album   = "";
    private String length  = "";

    private ArrayList<LyricItem> lyrics = new ArrayList<>();

    // ── Derivation tree (texto para visualizar) ────────────────────────────
    private StringBuilder tree = new StringBuilder();
    private int indent = 0;

    // ── API pública ────────────────────────────────────────────────────────

    /**
     * Parsea el archivo .lrc indicado.
     * @param path ruta al archivo
     * @throws Exception si hay error de I/O o de sintaxis
     */
    public void parse(String path) throws Exception {
        lyrics.clear();
        tree.setLength(0);
        indent = 0;

        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), "UTF-8"));

        ArrayList<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line.trim());
        }
        br.close();

        treeNode("archivo");
        indent++;

        int i = parseMetadata(lines, 0);
        parseLineas(lines, i);

        indent--;
    }

    // ── Productions ───────────────────────────────────────────────────────

    /**
     * Production: metadata → (meta_tag eol)*
     * Consume las líneas de metadato iniciales, retorna el índice
     * donde comienzan las líneas con timestamp.
     */
    private int parseMetadata(ArrayList<String> lines, int start) {
        treeNode("metadata");
        indent++;
        int i = start;
        while (i < lines.size()) {
            String l = lines.get(i);
            if (l.isEmpty()) { i++; continue; }

            Matcher mAr  = TAG_AR.matcher(l);
            Matcher mTi  = TAG_TI.matcher(l);
            Matcher mAl  = TAG_AL.matcher(l);
            Matcher mLen = TAG_LENGTH.matcher(l);
            Matcher mOth = TAG_OTHER.matcher(l);

            if (mAr.matches()) {
                artist = mAr.group(1).trim();
                treeNode("meta_tag → tag_ar : \"" + artist + "\"");
                i++;
            } else if (mTi.matches()) {
                title = mTi.group(1).trim();
                treeNode("meta_tag → tag_ti : \"" + title + "\"");
                i++;
            } else if (mAl.matches()) {
                album = mAl.group(1).trim();
                treeNode("meta_tag → tag_al : \"" + album + "\"");
                i++;
            } else if (mLen.matches()) {
                length = mLen.group(1).trim();
                treeNode("meta_tag → tag_length : \"" + length + "\"");
                i++;
            } else if (mOth.matches()) {
                treeNode("meta_tag → tag_other : \"" + l + "\"");
                i++;
            } else {
                // Ya no hay más metadatos
                break;
            }
        }
        indent--;
        return i;
    }

    /**
     * Production: lineas → linea+
     */
    private void parseLineas(ArrayList<String> lines, int start) {
        treeNode("lineas");
        indent++;
        for (int i = start; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.isEmpty()) continue;
            parseLinea(l, i + 1);
        }
        indent--;
    }

    /**
     * Production: linea → time_tag lyric_text? eol
     */
    private void parseLinea(String line, int lineNum) {
        Matcher m = TIME_TAG.matcher(line);
        if (!m.matches()) {
            // Línea sin timestamp → ignorar (puede ser blank line con otra tag)
            return;
        }
        int mm      = Integer.parseInt(m.group(1));
        int ss      = Integer.parseInt(m.group(2));
        int xx      = Integer.parseInt(m.group(3));
        String text = m.group(4).trim();

        long timeMs = (mm * 60L + ss) * 1000L + xx * 10L;

        treeNode("linea");
        indent++;
        treeNode(String.format("time_tag → [%02d:%02d.%02d]", mm, ss, xx));
        if (!text.isEmpty()) {
            treeNode("lyric_text → \"" + text + "\"");
        } else {
            treeNode("lyric_text → ε  (vacío)");
        }
        indent--;

        lyrics.add(new LyricItem(timeMs, text));
    }

    // ── Árbol de derivación ────────────────────────────────────────────────

    private void treeNode(String label) {
        for (int i = 0; i < indent; i++) tree.append("  ");
        tree.append("├─ ").append(label).append("\n");
    }

    /** Retorna el árbol de derivación como string. */
    public String getTree() {
        return tree.toString();
    }

    // ── Getters del repositorio ────────────────────────────────────────────

    public ArrayList<LyricItem> getLyrics() { return lyrics; }
    public String getArtist()  { return artist; }
    public String getTitle()   { return title;  }
    public String getAlbum()   { return album;  }
    public String getLength()  { return length; }

    // ── main de prueba ─────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java lyrics.LrcParser <archivo.lrc>");
            return;
        }
        LrcParser parser = new LrcParser();
        parser.parse(args[0]);

        System.out.println("=== Árbol de Derivación ===");
        System.out.println(parser.getTree());

        System.out.println("=== Metadatos ===");
        System.out.println("Artista : " + parser.getArtist());
        System.out.println("Título  : " + parser.getTitle());
        System.out.println("Álbum   : " + parser.getAlbum());

        System.out.println("\n=== Repositorio de Lyrics ===");
        for (LyricItem item : parser.getLyrics()) {
            System.out.println(item);
        }
    }
}
