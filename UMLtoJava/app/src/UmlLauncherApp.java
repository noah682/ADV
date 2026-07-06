package progProjekt.UMLtoJava.app.src;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UmlLauncherApp extends Application {

    private File selectedFile = null;
    private Label fileNameLabel;
    private Label filePathLabel;
    private Label dropHintLabel;
    private TextArea logArea;
    private Button startBtn;
    private Label statusLabel;
    private VBox dropZone;

    // Windows XP Luna-Farben
    private static final String XP_BG          = "#ECE9D8";
    private static final String XP_SILVER      = "#D4D0C8";
    private static final String XP_SILVER_DARK  = "#ADA99E";
    private static final String XP_BLUE_TITLE  = "linear-gradient(to right, #2255C4 0%, #4F80F7 40%, #3665E3 100%)";
    //private static final String XP_TITLE_TEXT  = "#FFFFFF";
    private static final String XP_GREEN       = "#418000";
    private static final String XP_BORDER_DARK = "#808080";
    private static final String XP_BORDER_LIGHT = "#FFFFFF";
    private static final String XP_GROUP_BG    = "#ECE9D8";

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + XP_BG + ";");

        root.getChildren().addAll(
            buildTitleBar(stage),
            buildMenuBar(),
            buildContent(),
            buildStatusBar()
        );

        Scene scene = new Scene(root, 520, 500);
        stage.setTitle("UML to Java Converter");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    // ─── Title Bar ────────────────────────────────────────────────────────────
    private HBox buildTitleBar(Stage stage) {
        HBox bar = new HBox();
        bar.setStyle(
            "-fx-background-color: " + XP_BLUE_TITLE + ";" +
            "-fx-padding: 4 6 4 8;" +
            "-fx-min-height: 28; -fx-max-height: 28;"
        );
        bar.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("⚙");
        icon.setStyle("-fx-text-fill: XP_TITLE_TEXT; -fx-font-size: 14px; -fx-padding: 0 6 0 0;");

        Label title = new Label("UML to Java Converter");
        title.setStyle(
            "-fx-text-fill: White; -fx-font-family: 'Tahoma', 'Arial';" +
            "-fx-font-size: 12px; -fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = buildTitleButton("✕", "#C75050", "#A83030");
        closeBtn.setOnAction(e -> Platform.exit());
        Button minBtn = buildTitleButton("─", "#5080C7", "#3665B0");
        minBtn.setOnAction(e -> stage.setIconified(true));

        bar.getChildren().addAll(icon, title, spacer, minBtn, closeBtn);

        // Fenster verschiebbar machen
        final double[] drag = {0, 0};
        bar.setOnMousePressed(e -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - drag[0]);
            stage.setY(e.getScreenY() - drag[1]);
        });
        return bar;
    }

    private Button buildTitleButton(String text, String bg, String hover) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: White;" +
            "-fx-font-size: 10px; -fx-min-width: 20; -fx-min-height: 18;" +
            "-fx-max-width: 20; -fx-max-height: 18; -fx-padding: 0;" +
            "-fx-border-color: rgba(255,255,255,0.5); -fx-border-width: 1;" +
            "-fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace(bg, hover)));
        b.setOnMouseExited(e -> b.setStyle(b.getStyle().replace(hover, bg)));
        return b;
    }

    // ─── Menu Bar ─────────────────────────────────────────────────────────────
    private HBox buildMenuBar() {
        HBox bar = new HBox(0);
        bar.setStyle(
            "-fx-background-color: " + XP_SILVER + ";" +
            "-fx-border-color: " + XP_SILVER_DARK + " transparent " + XP_BORDER_DARK + " transparent;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 1 4;"
        );
        for (String name : new String[]{"Datei", "Optionen", "Hilfe"}) {
            Label item = new Label(name);
            item.setStyle(
                "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
                "-fx-padding: 2 6; -fx-cursor: hand;"
            );
            item.setOnMouseEntered(e -> item.setStyle(item.getStyle() + "-fx-background-color: #316AC5; -fx-text-fill: White;"));
            item.setOnMouseExited(e -> item.setStyle(
                "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
                "-fx-padding: 2 6; -fx-cursor: hand;"
            ));
            bar.getChildren().add(item);
        }
        return bar;
    }

    // Main Content
    private VBox buildContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 12, 10, 12));

        content.getChildren().addAll(
            buildDropGroup(),
            buildButtonRow(),
            buildLogGroup()
        );
        return content;
    }

    private VBox buildDropGroup() {
        VBox group = buildGroupBox("UXF-Datei auswählen");

        dropZone = new VBox(8);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setMinHeight(130);
        dropZone.setPrefWidth(Double.MAX_VALUE);
        setDropZoneStyle(false, false);

        Label iconLabel = new Label("📂");
        iconLabel.setStyle("-fx-font-size: 32px;");

        dropHintLabel = new Label("Datei hier ablegen  oder  klicken zum Öffnen");
        dropHintLabel.setStyle(
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
            "-fx-text-fill: #666666;"
        );

        fileNameLabel = new Label();
        fileNameLabel.setStyle(
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 12px;" +
            "-fx-font-weight: bold; -fx-text-fill: " + XP_GREEN + ";"
        );
        fileNameLabel.setVisible(false);

        filePathLabel = new Label();
        filePathLabel.setStyle(
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 10px;" +
            "-fx-text-fill: #555555;"
        );
        filePathLabel.setVisible(false);

        dropZone.getChildren().addAll(iconLabel, dropHintLabel, fileNameLabel, filePathLabel);

        // Drag & Drop Events
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                setDropZoneStyle(true, false);
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> setDropZoneStyle(false, selectedFile != null));
        dropZone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            if (!files.isEmpty()) {
                File f = files.getFirst();
                if (f.getName().toLowerCase().endsWith(".uxf")) {
                    applyFile(f);
                    e.setDropCompleted(true);
                } else {
                    setStatus("Fehler: Nur .uxf-Dateien werden unterstützt.", true);
                    e.setDropCompleted(false);
                }
            }
            e.consume();
        });
        dropZone.setOnMouseClicked(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("UXF-Datei öffnen");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("UML-Dateien (*.uxf)", "*.uxf")
            );
            if (selectedFile != null)
                fc.setInitialDirectory(selectedFile.getParentFile());
            File f = fc.showOpenDialog(null);
            if (f != null) applyFile(f);
        });

        group.getChildren().add(dropZone);
        return group;
    }

    private void setDropZoneStyle(boolean dragging, boolean hasFile) {
        if (dragging) {
            dropZone.setStyle(
                "-fx-background-color: #EEF2FF;" +
                "-fx-border-color: #2255C4; -fx-border-width: 2;" +
                "-fx-border-style: dashed; -fx-padding: 12;"
            );
        } else if (hasFile) {
            dropZone.setStyle(
                "-fx-background-color: #F0FFF0;" +
                "-fx-border-color: " + XP_BORDER_DARK + " " + XP_BORDER_LIGHT +
                    " " + XP_BORDER_LIGHT + " " + XP_BORDER_DARK + ";" +
                "-fx-border-width: 2; -fx-padding: 12;"
            );
        } else {
            dropZone.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: " + XP_BORDER_DARK + " " + XP_BORDER_LIGHT +
                    " " + XP_BORDER_LIGHT + " " + XP_BORDER_DARK + ";" +
                "-fx-border-width: 2; -fx-padding: 12; -fx-cursor: hand;"
            );
        }
    }

    private void applyFile(File f) {
        selectedFile = f;
        dropHintLabel.setVisible(false);
        fileNameLabel.setText("✓  " + f.getName());
        fileNameLabel.setVisible(true);
        filePathLabel.setText(f.getParent());
        filePathLabel.setVisible(true);
        setDropZoneStyle(false, true);
        startBtn.setDisable(false);
        setStatus("Datei geladen: " + f.getName(), false);
    }

    private HBox buildButtonRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = buildXpButton("Zurücksetzen", false);
        clearBtn.setOnAction(e -> {
            selectedFile = null;
            dropHintLabel.setVisible(true);
            fileNameLabel.setVisible(false);
            filePathLabel.setVisible(false);
            setDropZoneStyle(false, false);
            startBtn.setDisable(true);
            logArea.clear();
            setStatus("Bereit", false);
        });

        startBtn = buildXpButton("▶  Start", true);
        startBtn.setDisable(true);
        startBtn.setOnAction(e -> runConversion());

        row.getChildren().addAll(clearBtn, startBtn);
        return row;
    }

    private Button buildXpButton(String text, boolean primary) {
        Button b = new Button(text);
        String bg = primary ? XP_GREEN : XP_SILVER;
        String fg = primary ? "white" : "#000000";
        String style =
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
            "-fx-min-width: 86; -fx-min-height: 24; -fx-padding: 4 12;" +
            "-fx-border-color: " + XP_BORDER_LIGHT + " " + XP_BORDER_DARK +
                " " + XP_BORDER_DARK + " " + XP_BORDER_LIGHT + ";" +
            "-fx-border-width: 2; -fx-cursor: hand;";
        b.setStyle(style);
        b.setOnMouseEntered(e -> {
            if (!b.isDisabled())
                b.setStyle(style + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 3, 0, 1, 1);");
        });
        b.setOnMouseExited(e -> b.setStyle(style));
        return b;
    }

    private VBox buildLogGroup() {
        VBox group = buildGroupBox("Ausgabe");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(155);
        logArea.setStyle(
            "-fx-font-family: 'Courier New', 'Consolas', monospace;" +
            "-fx-font-size: 11px;" +
            "-fx-control-inner-background: #000080;" +
            "-fx-text-fill: #C0C0C0;" +
            "-fx-background-color: #000080;" +
            "-fx-border-color: " + XP_BORDER_DARK + " " + XP_BORDER_LIGHT +
                " " + XP_BORDER_LIGHT + " " + XP_BORDER_DARK + ";" +
            "-fx-border-width: 2;"
        );
        group.getChildren().add(logArea);
        return group;
    }

    // ─── Status Bar ───────────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        HBox bar = new HBox(0);
        bar.setStyle(
            "-fx-background-color: " + XP_SILVER + ";" +
            "-fx-border-color: " + XP_BORDER_DARK + " transparent transparent transparent;" +
            "-fx-border-width: 1; -fx-padding: 2 8;"
        );
        statusLabel = new Label("Bereit");
        statusLabel.setStyle(
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
            "-fx-border-color: " + XP_BORDER_DARK + " " + XP_BORDER_LIGHT +
                " " + XP_BORDER_LIGHT + " " + XP_BORDER_DARK + ";" +
            "-fx-border-width: 1; -fx-padding: 0 4;"
        );
        bar.getChildren().add(statusLabel);
        return bar;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private VBox buildGroupBox(String title) {
        VBox box = new VBox(6);
        box.setStyle(
            "-fx-background-color: " + XP_GROUP_BG + ";" +
            "-fx-border-color: " + XP_SILVER_DARK + " " + XP_BORDER_LIGHT +
                " " + XP_BORDER_LIGHT + " " + XP_SILVER_DARK + ";" +
            "-fx-border-width: 1; -fx-padding: 8 8 8 8;"
        );
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-family: 'Tahoma', 'Arial'; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-text-fill: #000080;" +
            "-fx-background-color: " + XP_BG + "; -fx-padding: 0 4;"
        );
        box.getChildren().add(titleLabel);
        return box;
    }

    private void setStatus(String msg, boolean error) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle(statusLabel.getStyle().replace(
                error ? "#000000" : "#CC0000", error ? "#CC0000" : "#000000"
            ));
        });
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // ─── Conversion ───────────────────────────────────────────────────────────
    private void runConversion() {
        if (selectedFile == null) return;
        startBtn.setDisable(true);
        logArea.clear();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFolderName = selectedFile.getName().replaceFirst("\\.uxf$", "") + "_" + ts;
        File outputDir = new File(selectedFile.getParent(), outputFolderName);

        // UmlToJava.jar suchen
        File jar = findJar(selectedFile.getParentFile());
        if (jar == null) {
            log("[FEHLER] UmlToJava.jar nicht gefunden!");
            log("Erwartet neben der .uxf-Datei oder im Programmverzeichnis.");
            setStatus("Fehler: UmlToJava.jar nicht gefunden.", true);
            startBtn.setDisable(false);
            return;
        }

        log("[INFO]  Datei   : " + selectedFile.getName());
        log("[INFO]  JAR     : " + jar.getAbsolutePath());
        log("[INFO]  Ausgabe : " + outputDir.getAbsolutePath());
        log("[INFO]  Starte Konvertierung...");
        log("");
        setStatus("Konvertierung läuft...", false);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Absolute Pfade — kompatibel mit neuem JAR (args[0]/args[1])
                // und interaktivem Scanner-Modus (via stdin) des alten JARs.
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", jar.getAbsolutePath(),
                    selectedFile.getAbsolutePath(),
                    outputDir.getAbsolutePath()
                );
                pb.directory(selectedFile.getParentFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String l = line;
                        log(l);
                    }
                }

                int exit = proc.waitFor();
                log("");
                if (exit == 0 && outputDir.exists()) {
                    File[] files = outputDir.listFiles(f -> f.getName().endsWith(".java"));
                    int count = files != null ? files.length : 0;
                    log("[OK]    " + count + " Datei(en) generiert in:");
                    log("        " + outputDir.getAbsolutePath());
                    setStatus("Fertig — " + count + " Klasse(n) generiert.", false);
                } else if (exit != 0) {
                    log("[FEHLER] Prozess beendet mit Code " + exit);
                    setStatus("Fehler bei der Konvertierung (Code " + exit + ").", true);
                } else {
                    log("[WARN]  Ausgabeordner nicht gefunden, aber Prozess erfolgreich.");
                    setStatus("Konvertierung abgeschlossen.", false);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> startBtn.setDisable(false));
        task.setOnFailed(e -> {
            log("[FEHLER] " + task.getException().getMessage());
            setStatus("Unerwarteter Fehler.", true);
            startBtn.setDisable(false);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private File findJar(File dir) {
        String[] names = {"UmlToJava-new.jar", "UmlToJava.jar"};
        File userDir = new File(System.getProperty("user.dir"));
        File[] locations = {
            dir,
            new File(userDir, "app"),      // <projektroot>/app/
            userDir,                        // <projektroot>/
            dir.getParentFile(),
            dir.getParentFile() != null ? new File(dir.getParentFile(), "app") : null
        };
        for (String name : names) {
            for (File loc : locations) {
                if (loc == null) continue;
                File c = new File(loc, name);
                if (c.exists()) return c;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
