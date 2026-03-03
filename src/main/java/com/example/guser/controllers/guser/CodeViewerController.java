package com.example.guser.controllers.guser;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import services.guser.CodeBrowseService;
import services.guser.FileObjectService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CodeViewerController {

    @FXML private SplitPane codeSplitPane;

    @FXML private Button downloadZipBtn;
    @FXML private TreeView<ZipEntryVM> filesTreeView;

    @FXML private Label filePathLabel;
    @FXML private Button downloadFileBtn;

    @FXML private StackPane codeAreaHost;

    private CodeArea codeArea;

    private CodeBrowseService codeBrowseService;
    private FileObjectService fileObjectService;

    private String storageKey;
    private Path zipPath; // temp downloaded zip

    private String selectedEntryPath;

    public record ZipEntryVM(String name, String fullPath, boolean directory) {
        @Override public String toString() { return name; }
    }

    // --- Highlighting patterns (simple but effective) ---
    private static final Set<String> JAVA_KW = Set.of(
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
            "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
            "implements","import","instanceof","int","interface","long","native","new","package","private",
            "protected","public","return","short","static","strictfp","super","switch","synchronized",
            "this","throw","throws","transient","try","void","volatile","while"
    );

    private static final Set<String> JS_KW = Set.of(
            "break","case","catch","class","const","continue","debugger","default","delete","do","else",
            "export","extends","finally","for","function","if","import","in","instanceof","new","return",
            "super","switch","this","throw","try","typeof","var","void","while","with","yield","let","await"
    );

    private static final Set<String> PY_KW = Set.of(
            "False","None","True","and","as","assert","async","await","break","class","continue","def",
            "del","elif","else","except","finally","for","from","global","if","import","in","is","lambda",
            "nonlocal","not","or","pass","raise","return","try","while","with","yield"
    );

    private static final Set<String> SQL_KW = Set.of(
            "select","from","where","join","inner","left","right","full","on","group","by","order","limit",
            "insert","into","values","update","set","delete","create","table","primary","key","foreign",
            "constraint","drop","alter","add","index"
    );


    private static final Pattern STRING_P = Pattern.compile(
            "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"
    );
    private static final Pattern COMMENT_P = Pattern.compile(
            "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/" + "|" + "#[^\n]*"
    );
    private static final Pattern NUMBER_P = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
    private static final Pattern TYPE_P = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*\\b");
    @FXML private TextField searchField;
    @FXML private Label repoTitleLabel;


    @FXML
    private void initialize() {
        codeArea = new CodeArea();
        codeArea.setEditable(false);

        // Strong CSS hook
        codeArea.setId("cvArea");
        codeArea.getStyleClass().add("wsp-codeArea");

        // Line numbers
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // Make it fill the host
        codeArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        codeAreaHost.getChildren().setAll(codeArea);
        StackPane.setAlignment(codeArea, Pos.TOP_LEFT);

        // Buttons pop
        downloadZipBtn.getStyleClass().add("wsp-downloadBtn");
        downloadFileBtn.getStyleClass().add("wsp-downloadBtn");

        // Tree rendering with icons
        filesTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override protected void updateItem(ZipEntryVM v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }

                String icon = v.directory() ? "📁" : "📄";
                Label ico = new Label(icon);
                ico.getStyleClass().add("cv-treeIcon");

                Label name = new Label(v.name());
                name.getStyleClass().add("cv-treeName");


                HBox row = new HBox(8, ico, name);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                setGraphic(row);
                setText(null);
            }
        });

        filesTreeView.getSelectionModel().selectedItemProperty().addListener((obs, o, item) -> {
            if (item == null || item.getValue() == null) return;
            ZipEntryVM v = item.getValue();
            if (v.directory()) return;
            openFile(v.fullPath());
        });

        filePathLabel.setText("—");
        downloadFileBtn.setDisable(true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, q) -> applyTreeFilter(q));
        }
    }
    private void applyTreeFilter(String qRaw) {
        if (filesTreeView.getRoot() == null) return;

        String q = qRaw == null ? "" : qRaw.trim().toLowerCase();
        if (q.isEmpty()) {
            expandAll(filesTreeView.getRoot(), false);
            return;
        }

        filterTree(filesTreeView.getRoot(), q);
        expandAll(filesTreeView.getRoot(), true);
    }

    private boolean filterTree(TreeItem<ZipEntryVM> item, String q) {
        boolean matchSelf = item.getValue() != null && item.getValue().name() != null
                && item.getValue().name().toLowerCase().contains(q);

        boolean anyChild = false;
        for (TreeItem<ZipEntryVM> c : item.getChildren()) {
            anyChild |= filterTree(c, q);
        }

        // show branch if it matches or any child matches
        item.setExpanded(anyChild);

        // Hiding TreeItems cleanly is non-trivial; simplest UX: just expand matches and rely on quick navigation.
        // If you want real hide/show, we can rebuild a filtered root tree.
        return matchSelf || anyChild;
    }

    private void expandAll(TreeItem<?> root, boolean expanded) {
        root.setExpanded(expanded);
        for (TreeItem<?> c : root.getChildren()) expandAll(c, expanded);
    }



    public void init(CodeBrowseService codeBrowseService, FileObjectService fileObjectService, String storageKey) throws Exception {
        this.codeBrowseService = codeBrowseService;
        this.fileObjectService = fileObjectService;
        this.storageKey = storageKey;

        this.zipPath = codeBrowseService.downloadZipToTemp(storageKey);
        CodeBrowseService.ZipNode rootNode = codeBrowseService.buildTree(zipPath);

        TreeItem<ZipEntryVM> root = new TreeItem<>(new ZipEntryVM("root", "", true));
        root.setExpanded(true);
        for (CodeBrowseService.ZipNode child : rootNode.getChildren()) {
            root.getChildren().add(toTreeItem(child));
        }

        filesTreeView.setRoot(root);
        filesTreeView.setShowRoot(false);

        // Default open: README.md else first file
        Optional<String> readme = codeBrowseService.findReadmeAtRoot(rootNode);
        if (readme.isPresent()) {
            openFile(readme.get());
            return;
        }
        codeBrowseService.findFirstFile(rootNode).ifPresent(this::openFile);
        if (repoTitleLabel != null) repoTitleLabel.setText("Repository");

    }

    private TreeItem<ZipEntryVM> toTreeItem(CodeBrowseService.ZipNode n) {
        TreeItem<ZipEntryVM> item = new TreeItem<>(new ZipEntryVM(n.getName(), n.getFullPath(), n.isDirectory()));
        for (CodeBrowseService.ZipNode c : n.getChildren()) item.getChildren().add(toTreeItem(c));
        return item;
    }

    private void openFile(String entryPath) {
        try {
            String content = codeBrowseService.readTextFile(zipPath, entryPath);
            selectedEntryPath = entryPath;

            filePathLabel.setText(entryPath);
            downloadFileBtn.setDisable(false);

            codeArea.replaceText(content);
            applyHighlighting(content, entryPath);
            codeArea.moveTo(0);
            codeArea.requestFollowCaret();



        } catch (Exception e) {
            filePathLabel.setText(entryPath);
            downloadFileBtn.setDisable(true);
            codeArea.replaceText("Failed to open file: " + e.getMessage());
        }

    }

    private void applyHighlighting(String text, String entryPath) {
        String ext = extFromPath(entryPath);

        Set<String> kwSet;
        switch (ext) {
            case "java" -> kwSet = JAVA_KW;
            case "js", "ts" -> kwSet = JS_KW;
            case "py" -> kwSet = PY_KW;
            case "sql" -> kwSet = SQL_KW;
            case "xml", "fxml", "html" -> kwSet = Collections.emptySet(); // only strings/comments
            default -> {
                // No highlight for md/json/txt/etc if you want it calmer
                codeArea.setStyleSpans(0, emptySpans(text.length()));
                return;
            }
        }

        StyleSpans<Collection<String>> spans = computeSpans(text, kwSet);
        codeArea.setStyleSpans(0, spans);
    }

    private StyleSpans<Collection<String>> emptySpans(int len) {
        return new StyleSpansBuilder<Collection<String>>()
                .add(Collections.emptyList(), len)
                .create();
    }

    private StyleSpans<Collection<String>> computeSpans(String text, Set<String> kwSet) {
        String keywordPattern = kwSet.isEmpty()
                ? "(?!)"
                : "\\b(" + String.join("|", kwSet) + ")\\b";

        Pattern pattern = Pattern.compile(
                "(?<COM>" + COMMENT_P.pattern() + ")"
                        + "|(?<STR>" + STRING_P.pattern() + ")"
                        + "|(?<NUM>" + NUMBER_P.pattern() + ")"
                        + "|(?<KW>"  + keywordPattern + ")"
                        + "|(?<TYP>" + TYPE_P.pattern() + ")"
        );

        Matcher matcher = pattern.matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("COM") != null ? "com" :
                            matcher.group("STR") != null ? "str" :
                                    matcher.group("NUM") != null ? "num" :
                                            matcher.group("KW")  != null ? "kw"  :
                                                    matcher.group("TYP") != null ? "typ" :
                                                            null;

            b.add(Collections.emptyList(), matcher.start() - last);
            b.add(styleClass == null ? Collections.emptyList() : Collections.singleton(styleClass),
                    matcher.end() - matcher.start());
            last = matcher.end();
        }

        b.add(Collections.emptyList(), text.length() - last);
        return b.create();
    }


    private static String extFromPath(String p) {
        if (p == null) return "";
        int i = p.lastIndexOf('.');
        if (i < 0 || i == p.length() - 1) return "";
        return p.substring(i + 1).toLowerCase();
    }

    @FXML
    private void onDownloadZip() {
        try {
            if (storageKey == null || storageKey.isBlank()) throw new IllegalStateException("Missing storageKey.");

            String url = fileObjectService.presignedDownloadUrl(storageKey, Duration.ofMinutes(10));

            FileChooser fc = new FileChooser();
            fc.setTitle("Save ZIP");
            fc.setInitialFileName("repo.zip");
            File dest = fc.showSaveDialog(codeSplitPane.getScene().getWindow());
            if (dest == null) return;

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(dest.toPath()));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new IllegalStateException("Download failed (HTTP " + res.statusCode() + ")");
            }
        } catch (Exception e) {
            codeArea.replaceText("Download ZIP failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDownloadFile() {
        try {
            if (zipPath == null) throw new IllegalStateException("ZIP not loaded.");
            if (selectedEntryPath == null || selectedEntryPath.isBlank()) throw new IllegalStateException("No file selected.");

            FileChooser fc = new FileChooser();
            fc.setTitle("Save file");
            fc.setInitialFileName(Paths.get(selectedEntryPath).getFileName().toString());
            File dest = fc.showSaveDialog(codeSplitPane.getScene().getWindow());
            if (dest == null) return;

            byte[] bytes = readBytesFromZip(zipPath, selectedEntryPath);
            Files.write(dest.toPath(), bytes);
        } catch (Exception e) {
            codeArea.replaceText("Download file failed: " + e.getMessage());
        }
    }

    private byte[] readBytesFromZip(Path zip, String entryPath) throws IOException {
        String wanted = normalize(entryPath);

        try (InputStream in = Files.newInputStream(zip);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = normalize(e.getName());
                if (wanted.equals(name)) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new FileNotFoundException("File not found in zip: " + entryPath);
    }

    private static String normalize(String p) {
        if (p == null) return "";
        p = p.replace("\\", "/").trim();
        while (p.startsWith("/")) p = p.substring(1);
        return p;
    }

    public void dispose() {
        try { if (zipPath != null) Files.deleteIfExists(zipPath); } catch (Exception ignored) {}
    }
}
