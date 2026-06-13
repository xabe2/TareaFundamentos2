package lyrics;

/**
 * Representa un ítem de lyric con su tiempo (en ms) y texto asociado.
 * Tarea 2 - Fundamentos de Ciencias de la Computación
 */
public class LyricItem {

    private long timeMs;   // tiempo en milisegundos
    private String text;   // texto del lyric

    public LyricItem(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text   = text;
    }

    /** Convierte mm:ss.xx a milisegundos */
    public static long parseTime(String tag) {
        // tag tiene formato [mm:ss.xx]
        // Remover corchetes
        String inner = tag.replaceAll("[\\[\\]]", "").trim();
        // inner = "mm:ss.xx"
        String[] parts = inner.split(":");
        int mm = Integer.parseInt(parts[0]);
        String[] secs = parts[1].split("\\.");
        int ss = Integer.parseInt(secs[0]);
        int xx = Integer.parseInt(secs[1]);
        return (mm * 60L + ss) * 1000L + xx * 10L;
    }

    public long getTimeMs()  { return timeMs; }
    public String getText()  { return text;   }

    @Override
    public String toString() {
        return String.format("[%d ms] %s", timeMs, text);
    }
}
