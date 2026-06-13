import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import lyrics.LrcParser;
import lyrics.LyricItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Reproductor MP3 con sincronización de letras (.lrc).
 *
 * Arquitectura (3 etapas de la tarea):
 *   Etapa 1 → LrcParser (gramática LR) construye árbol de derivación
 *   Etapa 2 → Visitador almacena LyricItems en ArrayList (repositorio)
 *   Etapa 3 → Reproductor usa Timer/Reminder para mostrar letra en pantalla
 *
 * Tarea 2 - Fundamentos de Ciencias de la Computación
 * Semestre Otoño 2026
 */
public class Reproductor implements BasicPlayerListener {

    // ── BasicPlayer (Etapa 3: reproductor MP3) ─────────────────────────────
    private BasicPlayer player;

    // ── Repositorio de letras (Etapa 2: visitador) ─────────────────────────
    private ArrayList<LyricItem> lyrics = new ArrayList<>();
    private int currentLyricIndex = 0;

    // ── Timer para sincronización ──────────────────────────────────────────
    private Timer syncTimer;
    private long startTimeSystem; // tiempo del sistema cuando comenzó la canción
    private long pausedAt;        // ms acumulados al momento de pausar
    private boolean paused = false;

    // ── Estado del reproductor ─────────────────────────────────────────────
    private String currentMp3  = "";
    private String currentLrc  = "";
    private boolean hasLyrics  = false;

    // ── GUI Components ────────────────────────────────────────────────────
    private JFrame frame;
    private JLabel lblTitle;
    private JLabel lblArtist;
    private JLabel lblCurrentLyric;
    private JLabel lblNextLyric;
    private JButton btnPlay;
    private JButton btnPause;
    private JButton btnStop;
    private JButton btnOpenMp3;
    private JButton btnOpenLrc;
    private JTextField txtMp3Path;
    private JTextField txtLrcPath;
    private JTextArea txtTreeArea;
    private JLabel lblStatus;
    private JLabel lblTimer;
    private Timer uiTimer;

    // ── Paleta de colores ─────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(18,  18,  18);
    private static final Color BG_CARD    = new Color(30,  30,  30);
    private static final Color BG_CTRL    = new Color(40,  40,  40);
    private static final Color ACCENT     = new Color(29, 185, 84);   // verde Spotify
    private static final Color ACCENT2    = new Color(100, 220, 140);
    private static final Color TEXT_PRI   = new Color(255, 255, 255);
    private static final Color TEXT_SEC   = new Color(180, 180, 180);
    private static final Color TEXT_DIM   = new Color(100, 100, 100);
    private static final Color BTN_RED    = new Color(200,  50,  50);

    // ══════════════════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════════════════
    public Reproductor() {
        player = new BasicPlayer();
        player.addBasicPlayerListener(this);
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Métodos de reproducción (Etapa 3)
    // ══════════════════════════════════════════════════════════════════════

    public void abrirFichero(String ruta) throws Exception {
        player.open(new File(ruta));
        currentMp3 = ruta;
    }

    public void play() throws Exception {
        player.play();
        paused = false;
        startTimeSystem = System.currentTimeMillis();
        if (hasLyrics) {
            currentLyricIndex = 0;
            scheduleAllReminders(0);
        }
        startUITimer();
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
        setStatus("▶  Reproduciendo: " + new File(currentMp3).getName());
    }

    public void pause() throws Exception {
        player.pause();
        paused = true;
        pausedAt = System.currentTimeMillis() - startTimeSystem;
        cancelReminders();
        if (uiTimer != null) uiTimer.cancel();
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        setStatus("⏸  En pausa");
    }

    public void reanudar() throws Exception {
        player.resume();
        paused = false;
        startTimeSystem = System.currentTimeMillis() - pausedAt;
        if (hasLyrics) {
            scheduleAllReminders(pausedAt);
        }
        startUITimer();
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        setStatus("▶  Reproduciendo: " + new File(currentMp3).getName());
    }

    public void stop() throws Exception {
        player.stop();
        paused = false;
        cancelReminders();
        if (uiTimer != null) uiTimer.cancel();
        currentLyricIndex = 0;
        clearLyricDisplay();
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        lblTimer.setText("00:00");
        setStatus("⏹  Detenido");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Carga de letra (Etapa 1 + Etapa 2)
    // ══════════════════════════════════════════════════════════════════════

    public void cargarLrc(String ruta) {
        try {
            // Etapa 1: parsear → árbol de derivación
            LrcParser parser = new LrcParser();
            parser.parse(ruta);

            // Etapa 2: repositorio de letras
            lyrics = parser.getLyrics();
            hasLyrics = !lyrics.isEmpty();
            currentLrc = ruta;

            // Actualizar metadatos en GUI
            String titulo  = parser.getTitle().isEmpty()
                    ? new File(ruta).getName().replace(".lrc","") : parser.getTitle();
            String artista = parser.getArtist().isEmpty() ? "—" : parser.getArtist();
            String alb     = parser.getAlbum().isEmpty()  ? "—" : parser.getAlbum();

            lblTitle.setText(titulo);
            lblArtist.setText(artista + (alb.equals("—") ? "" : "  ·  " + alb));

            // Mostrar árbol de derivación
            txtTreeArea.setText(parser.getTree());
            txtTreeArea.setCaretPosition(0);

            setStatus("✓ Letra cargada: " + lyrics.size() + " líneas — " + titulo);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error al parsear .lrc:\n" + ex.getMessage(),
                    "Error de Parser", JOptionPane.ERROR_MESSAGE);
            hasLyrics = false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Reminder / Timer (Etapa 3)
    // ══════════════════════════════════════════════════════════════════════

    /** Programa un Reminder por cada LyricItem no pasado todavía. */
    private void scheduleAllReminders(long elapsedMs) {
        cancelReminders();
        syncTimer = new Timer("lyric-sync", true);
        for (int i = 0; i < lyrics.size(); i++) {
            LyricItem item = lyrics.get(i);
            long delay = item.getTimeMs() - elapsedMs;
            if (delay >= 0) {
                final int idx = i;
                new Reminder(delay, item) {
                    @Override
                    public void onTime() {
                        showLyric(idx);
                    }
                };
            }
        }
    }

    private void cancelReminders() {
        if (syncTimer != null) {
            syncTimer.cancel();
            syncTimer = null;
        }
    }

    /** Actualiza la GUI con el lyric del índice dado. */
    private void showLyric(int idx) {
        SwingUtilities.invokeLater(() -> {
            currentLyricIndex = idx;
            LyricItem cur = lyrics.get(idx);

            if (cur.getText().isEmpty()) {
                lblCurrentLyric.setText("♪");
            } else {
                lblCurrentLyric.setText(cur.getText());
            }

            // Línea siguiente (preview)
            if (idx + 1 < lyrics.size()) {
                String next = lyrics.get(idx + 1).getText();
                lblNextLyric.setText(next.isEmpty() ? "" : next);
            } else {
                lblNextLyric.setText("");
            }
        });
    }

    private void clearLyricDisplay() {
        SwingUtilities.invokeLater(() -> {
            lblCurrentLyric.setText("♪");
            lblNextLyric.setText("");
        });
    }

    // ── Inner class Reminder (como pedido en la tarea) ─────────────────────
    public abstract class Reminder {
        protected Timer timer;

        public Reminder(long delayMs, LyricItem item) {
            timer = syncTimer; // usa el timer global de la clase padre
            if (timer != null) {
                timer.schedule(new TimerTask() {
                    public void run() { onTime(); }
                }, delayMs);
            }
        }

        public abstract void onTime();
    }

    // ── Timer para el contador de tiempo en pantalla ───────────────────────
    private void startUITimer() {
        if (uiTimer != null) uiTimer.cancel();
        uiTimer = new Timer("ui-timer", true);
        uiTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!paused) {
                    long elapsed = System.currentTimeMillis() - startTimeSystem;
                    long mins = elapsed / 60000;
                    long secs = (elapsed % 60000) / 1000;
                    SwingUtilities.invokeLater(() ->
                            lblTimer.setText(String.format("%02d:%02d", mins, secs)));
                }
            }
        }, 0, 500);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BasicPlayerListener
    // ══════════════════════════════════════════════════════════════════════
    @Override public void opened(Object o, Map m) {}
    @Override public void progress(int i, long l, byte[] b, Map m) {}
    @Override public void stateUpdated(BasicPlayerEvent e) {
        if (e.getCode() == BasicPlayerEvent.EOM) {
            // Fin de la canción
            SwingUtilities.invokeLater(() -> {
                try { stop(); } catch (Exception ex) {}
                setStatus("✓ Fin de la canción");
            });
        }
    }
    @Override public void setController(javazoom.jlgui.basicplayer.BasicController c) {}

    // ══════════════════════════════════════════════════════════════════════
    //  Construcción de la GUI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {
        frame = new JFrame("Reproductor MP3 + LRC");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 680);
        frame.setMinimumSize(new Dimension(700, 560));
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout(0, 0));

        // ── Panel superior: título / artista / timer ───────────────────────
        JPanel topPanel = new JPanel(new BorderLayout(10, 4));
        topPanel.setBackground(BG_CARD);
        topPanel.setBorder(new EmptyBorder(18, 24, 14, 24));

        lblTitle = styledLabel("Ninguna canción cargada", 22, Font.BOLD, TEXT_PRI);
        lblArtist = styledLabel("—", 14, Font.PLAIN, TEXT_SEC);
        lblTimer = styledLabel("00:00", 20, Font.BOLD, ACCENT);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(lblTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lblArtist);

        topPanel.add(titleBox, BorderLayout.CENTER);
        topPanel.add(lblTimer, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        // ── Panel central: letra actual ────────────────────────────────────
        JPanel lyricPanel = new JPanel();
        lyricPanel.setBackground(BG_DARK);
        lyricPanel.setLayout(new BoxLayout(lyricPanel, BoxLayout.Y_AXIS));
        lyricPanel.setBorder(new EmptyBorder(10, 24, 10, 24));

        lblCurrentLyric = styledLabel("♪", 26, Font.BOLD, ACCENT);
        lblCurrentLyric.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCurrentLyric.setHorizontalAlignment(SwingConstants.CENTER);

        lblNextLyric = styledLabel("", 16, Font.PLAIN, TEXT_DIM);
        lblNextLyric.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblNextLyric.setHorizontalAlignment(SwingConstants.CENTER);

        lyricPanel.add(Box.createVerticalGlue());
        lyricPanel.add(lblCurrentLyric);
        lyricPanel.add(Box.createVerticalStrut(12));
        lyricPanel.add(lblNextLyric);
        lyricPanel.add(Box.createVerticalGlue());

        // ── Panel árbol de derivación (collapsible) ────────────────────────
        txtTreeArea = new JTextArea(8, 40);
        txtTreeArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtTreeArea.setBackground(new Color(20, 20, 20));
        txtTreeArea.setForeground(new Color(150, 200, 150));
        txtTreeArea.setCaretColor(ACCENT);
        txtTreeArea.setEditable(false);
        txtTreeArea.setText("[ El árbol de derivación aparecerá aquí tras cargar un .lrc ]");

        JScrollPane treeScroll = new JScrollPane(txtTreeArea);
        treeScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60,60,60)),
                "Árbol de Derivación (Etapa 1)",
                0, 0,
                new Font("SansSerif", Font.BOLD, 11),
                TEXT_SEC));
        treeScroll.setBackground(BG_DARK);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                lyricPanel, treeScroll);
        splitPane.setDividerLocation(200);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(null);
        frame.add(splitPane, BorderLayout.CENTER);

        // ── Panel inferior: controles ──────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(BG_CARD);
        bottomPanel.setBorder(new EmptyBorder(12, 18, 16, 18));

        // Fila de archivos
        JPanel filesPanel = new JPanel(new GridLayout(2, 1, 4, 6));
        filesPanel.setOpaque(false);

        // MP3
        txtMp3Path = styledTextField("Ruta del archivo .mp3");
        btnOpenMp3 = styledButton("📁 MP3", BG_CTRL, TEXT_PRI);
        btnOpenMp3.addActionListener(e -> chooseMp3());
        JPanel mp3Row = fileRow("MP3:", txtMp3Path, btnOpenMp3);
        filesPanel.add(mp3Row);

        // LRC
        txtLrcPath = styledTextField("Ruta del archivo .lrc (opcional)");
        btnOpenLrc = styledButton("📁 LRC", BG_CTRL, TEXT_PRI);
        btnOpenLrc.addActionListener(e -> chooseLrc());
        JPanel lrcRow = fileRow("LRC:", txtLrcPath, btnOpenLrc);
        filesPanel.add(lrcRow);

        // Fila de controles de reproducción
        btnPlay  = styledButton("▶ Play",  ACCENT,   BG_DARK);
        btnPause = styledButton("⏸ Pausa", BG_CTRL,  TEXT_PRI);
        btnStop  = styledButton("⏹ Stop",  BTN_RED,  TEXT_PRI);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);

        btnPlay.addActionListener(e -> doPlay());
        btnPause.addActionListener(e -> doPause());
        btnStop.addActionListener(e -> doStop());

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        ctrlRow.setOpaque(false);
        ctrlRow.add(btnPlay);
        ctrlRow.add(btnPause);
        ctrlRow.add(btnStop);

        lblStatus = styledLabel("Listo. Cargue un archivo MP3.", 12, Font.PLAIN, TEXT_DIM);

        bottomPanel.add(filesPanel, BorderLayout.NORTH);
        bottomPanel.add(ctrlRow,    BorderLayout.CENTER);
        bottomPanel.add(lblStatus,  BorderLayout.SOUTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── Acciones de botones ────────────────────────────────────────────────

    private void chooseMp3() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Abrir archivo MP3");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3", "mp3"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            txtMp3Path.setText(path);
            try {
                abrirFichero(path);
                setStatus("✓ MP3 cargado: " + fc.getSelectedFile().getName());
                btnPlay.setEnabled(true);
                // Intentar cargar .lrc con mismo nombre automáticamente
                String lrcAuto = path.replaceAll("(?i)\\.mp3$", ".lrc");
                File lrcFile = new File(lrcAuto);
                if (lrcFile.exists()) {
                    txtLrcPath.setText(lrcAuto);
                    cargarLrc(lrcAuto);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "No se pudo abrir el MP3:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void chooseLrc() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Abrir archivo LRC");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("LRC Lyrics", "lrc"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            txtLrcPath.setText(path);
            cargarLrc(path);
        }
    }

    private void doPlay() {
        if (currentMp3.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Primero cargue un archivo MP3.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (paused) {
                reanudar();
            } else {
                play();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error al reproducir:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doPause() {
        try { pause(); } catch (Exception ex) {}
    }

    private void doStop() {
        try { stop(); } catch (Exception ex) {}
    }

    // ── Helpers de GUI ─────────────────────────────────────────────────────

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(msg));
    }

    private JLabel styledLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private JButton styledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 38));
        return b;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setBackground(new Color(50, 50, 50));
        tf.setForeground(TEXT_SEC);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                new EmptyBorder(4, 8, 4, 8)));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return tf;
    }

    private JPanel fileRow(String label, JTextField tf, JButton btn) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel lbl = styledLabel(label, 12, Font.BOLD, TEXT_SEC);
        lbl.setPreferredSize(new Dimension(34, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(tf,  BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        btn.setPreferredSize(new Dimension(90, 28));
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  main
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Establecer look and feel oscuro
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            Reproductor rep = new Reproductor();

            // Si se pasan argumentos por línea de comandos
            if (args.length >= 1) {
                try {
                    rep.abrirFichero(args[0]);
                    rep.txtMp3Path.setText(args[0]);
                    if (args.length >= 2) {
                        rep.txtLrcPath.setText(args[1]);
                        rep.cargarLrc(args[1]);
                    }
                } catch (Exception ex) {
                    System.err.println("Error al cargar: " + ex.getMessage());
                }
            }
        });
    }
}
