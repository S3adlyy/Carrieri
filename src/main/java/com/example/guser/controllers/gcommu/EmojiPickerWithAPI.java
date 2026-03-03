package com.example.guser.controllers.gcommu;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EmojiPickerWithAPI extends Dialog<String> {

    private TextField searchField;
    private GridPane emojiGrid;
    private List<String> allEmojis = new ArrayList<>();
    private ToggleGroup categoryGroup = new ToggleGroup();
    private String currentCategory = "all";

    // Couleurs du thème violet
    private final String PURPLE_PRIMARY = "#8B5CF6";
    private final String PURPLE_LIGHT = "#EDE9FE";
    private final String PURPLE_DARK = "#6D28D9";
    private final String GRAY_BG = "#F9FAFB";
    private final String GRAY_HOVER = "#F3F4F6";

    public EmojiPickerWithAPI() {
        setTitle("Sélecteur d'emojis");
        setHeaderText("Choisissez un emoji");

        // Style de la boîte de dialogue
        DialogPane dialogPane = getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        dialogPane.getScene().getWindow().setOnShown(e -> {
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.setWidth(500);
            stage.setHeight(500);
        });

        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: white; -fx-background-radius: 20;");

        // Barre de recherche stylisée
        HBox searchBox = createSearchBox();

        // Catégories d'emojis
        HBox categoryBar = createCategoryBar();

        // Grille d'emojis
        emojiGrid = new GridPane();
        emojiGrid.setHgap(8);
        emojiGrid.setVgap(8);
        emojiGrid.setStyle("-fx-padding: 10; -fx-background-color: " + GRAY_BG + "; -fx-background-radius: 15;");

        ScrollPane scrollPane = new ScrollPane(emojiGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        mainContent.getChildren().addAll(searchBox, categoryBar, scrollPane);
        dialogPane.setContent(mainContent);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        // Style du bouton fermer
        Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        closeButton.setStyle(
            "-fx-background-color: " + PURPLE_LIGHT + ";" +
                "-fx-text-fill: " + PURPLE_DARK + ";" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 8 20;"
        );

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterEmojis(newVal.toLowerCase(), currentCategory);
        });

        // Charger les emojis
        loadEmojis();
    }

    private HBox createSearchBox() {
        HBox box = new HBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setStyle(
            "-fx-background-color: " + GRAY_BG + ";" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 5 15;"
        );

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #9CA3AF;");

        searchField = new TextField();
        searchField.setPromptText("Rechercher un emoji...");
        searchField.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10 5;"
        );
        searchField.setPrefWidth(300);

        box.getChildren().addAll(searchIcon, searchField);
        return box;
    }

    private HBox createCategoryBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding: 5 0;");

        String[] categories = {"Tous", "😊 Sourires", "❤️ Cœurs", "👍 Gestes", "🐶 Animaux", "🍔 Nourriture"};
        String[] categoryIds = {"all", "smileys", "hearts", "gestures", "animals", "food"};

        for (int i = 0; i < categories.length; i++) {
            ToggleButton btn = new ToggleButton(categories[i]);
            btn.setUserData(categoryIds[i]);
            btn.setToggleGroup(categoryGroup);
            btn.setStyle(
                "-fx-background-color: transparent;" +
                    "-fx-text-fill: #6B7280;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 8 15;" +
                    "-fx-background-radius: 20;"
            );

            btn.setOnAction(e -> {
                currentCategory = (String) btn.getUserData();
                filterEmojis(searchField.getText().toLowerCase(), currentCategory);
            });

            if (i == 0) {
                btn.setSelected(true);
                btn.setStyle(
                    "-fx-background-color: " + PURPLE_PRIMARY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 15;" +
                        "-fx-background-radius: 20;"
                );
            }

            bar.getChildren().add(btn);
        }

        // Changement de style quand sélectionné
        categoryGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                for (javafx.scene.Node node : bar.getChildren()) {
                    if (node instanceof ToggleButton) {
                        ToggleButton btn = (ToggleButton) node;
                        if (btn == newVal) {
                            btn.setStyle(
                                "-fx-background-color: " + PURPLE_PRIMARY + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 8 15;" +
                                    "-fx-background-radius: 20;"
                            );
                        } else {
                            btn.setStyle(
                                "-fx-background-color: transparent;" +
                                    "-fx-text-fill: #6B7280;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 8 15;" +
                                    "-fx-background-radius: 20;"
                            );
                        }
                    }
                }
            }
        });

        return bar;
    }

    private void loadEmojis() {
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://api.github.com/emojis");
                parseGitHubEmojis(jsonResponse);
            } catch (Exception e) {
                e.printStackTrace();
                loadLocalEmojis();
            }

            javafx.application.Platform.runLater(() -> {
                filterEmojis("", "all");
            });
        }).start();
    }

    private String fetchFromAPI(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        return content.toString();
    }

    private void parseGitHubEmojis(String json) {
        allEmojis.clear();

        String[][] emojiMap = {
            {"smile", "😄"}, {"smiley", "😃"}, {"grinning", "😀"}, {"blush", "😊"},
            {"wink", "😉"}, {"heart_eyes", "😍"}, {"kissing_heart", "😘"}, {"kissing", "😗"},
            {"kissing_smiling_eyes", "😙"}, {"kissing_closed_eyes", "😚"}, {"relaxed", "☺️"},
            {"satisfied", "😆"}, {"grin", "😁"}, {"laughing", "😆"}, {"stuck_out_tongue", "😛"},
            {"stuck_out_tongue_closed_eyes", "😝"}, {"stuck_out_tongue_winking_eye", "😜"},
            {"joy", "😂"}, {"sob", "😭"}, {"sleeping", "😴"}, {"disappointed", "😞"},
            {"worried", "😟"}, {"angry", "😠"}, {"rage", "😡"}, {"cry", "😢"},
            {"persevere", "😣"}, {"triumph", "😤"}, {"fearful", "😨"}, {"frowning", "😦"},
            {"anguished", "😧"}, {"grimacing", "😬"}, {"confounded", "😖"}, {"tired_face", "😫"},
            {"weary", "😩"}, {"expressionless", "😑"}, {"unamused", "😒"}, {"sweat_smile", "😅"},
            {"sweat", "😓"}, {"yum", "😋"}, {"mask", "😷"}, {"sunglasses", "😎"},
            {"heart", "❤️"}, {"broken_heart", "💔"}, {"kiss", "💋"}, {"+1", "👍"},
            {"-1", "👎"}, {"ok_hand", "👌"}, {"clap", "👏"}, {"wave", "👋"},
            {"pray", "🙏"}, {"muscle", "💪"}, {"fire", "🔥"}, {"star", "⭐"},
            {"dog", "🐶"}, {"cat", "🐱"}, {"mouse", "🐭"}, {"hamster", "🐹"},
            {"rabbit", "🐰"}, {"fox", "🦊"}, {"bear", "🐻"}, {"panda", "🐼"},
            {"koala", "🐨"}, {"frog", "🐸"}, {"monkey", "🐒"}, {"chicken", "🐔"},
            {"penguin", "🐧"}, {"bird", "🐦"}, {"baby_chick", "🐤"}, {"hatching_chick", "🐣"},
            {"hatched_chick", "🐥"}, {"wolf", "🐺"}, {"boar", "🐗"}, {"horse", "🐴"},
            {"unicorn", "🦄"}, {"bee", "🐝"}, {"bug", "🐛"}, {"butterfly", "🦋"},
            {"snail", "🐌"}, {"shell", "🐚"}, {"crab", "🦀"}, {"shrimp", "🦐"},
            {"squid", "🦑"}, {"octopus", "🐙"}, {"dolphin", "🐬"}, {"fish", "🐟"},
            {"tropical_fish", "🐠"}, {"blowfish", "🐡"}, {"shark", "🦈"}, {"whale", "🐋"},
            {"whale2", "🐳"}, {"crocodile", "🐊"}, {"turtle", "🐢"}, {"lizard", "🦎"},
            {"snake", "🐍"}, {"dragon", "🐉"}, {"dragon_face", "🐲"}, {"sauropod", "🦕"},
            {"t-rex", "🦖"}, {"pizza", "🍕"}, {"hamburger", "🍔"}, {"fries", "🍟"},
            {"hotdog", "🌭"}, {"popcorn", "🍿"}, {"salt", "🧂"}, {"bacon", "🥓"},
            {"egg", "🥚"}, {"fried_egg", "🍳"}, {"pancakes", "🥞"}, {"donut", "🍩"},
            {"cookie", "🍪"}, {"chocolate_bar", "🍫"}, {"candy", "🍬"}, {"lollipop", "🍭"},
            {"cake", "🎂"}, {"cupcake", "🧁"}, {"pie", "🥧"}, {"icecream", "🍦"}
        };

        for (String[] pair : emojiMap) {
            allEmojis.add(pair[1]);
        }
    }

    private void loadLocalEmojis() {
        allEmojis.clear();
        String[] fallback = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
            "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒",
            "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩",
            "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘",
            "👋", "🤚", "🖐️", "✋", "👌", "🤏", "🤌", "👈", "👉", "👆",
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐸",
            "🍕", "🍔", "🍟", "🌭", "🍿", "🥓", "🍳", "🥞", "🍩", "🍪"
        };

        for (String emoji : fallback) {
            allEmojis.add(emoji);
        }
    }

    private void filterEmojis(String filter, String category) {
        List<String> filtered = new ArrayList<>();

        for (String emoji : allEmojis) {
            boolean matchFilter = filter.isEmpty() || emoji.contains(filter);
            boolean matchCategory = matchCategory(emoji, category);

            if (matchFilter && matchCategory) {
                filtered.add(emoji);
            }
        }

        displayEmojis(filtered);
    }

    private boolean matchCategory(String emoji, String category) {
        if (category.equals("all")) return true;

        // Catégories basées sur les codes Unicode
        int code = emoji.codePointAt(0);

        switch (category) {
            case "smileys":
                return (code >= 0x1F600 && code <= 0x1F64F);
            case "hearts":
                return emoji.contains("❤️") || emoji.contains("🧡") || emoji.contains("💛") ||
                    emoji.contains("💚") || emoji.contains("💙") || emoji.contains("💜") ||
                    emoji.contains("🖤") || emoji.contains("🤍") || emoji.contains("🤎") ||
                    emoji.contains("💔");
            case "gestures":
                return (code >= 0x1F44A && code <= 0x1F44F) || (code >= 0x1F64C && code <= 0x1F64F);
            case "animals":
                return (code >= 0x1F400 && code <= 0x1F43F);
            case "food":
                return (code >= 0x1F32D && code <= 0x1F37F);
            default:
                return true;
        }
    }

    private void displayEmojis(List<String> emojis) {
        emojiGrid.getChildren().clear();

        int col = 0;
        int row = 0;
        int maxCols = 6;

        for (String emoji : emojis) {
            Button btn = new Button(emoji);
            btn.setStyle(
                "-fx-font-size: 32px;" +
                    "-fx-min-width: 60;" +
                    "-fx-min-height: 60;" +
                    "-fx-max-width: 60;" +
                    "-fx-max-height: 60;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 15;" +
                    "-fx-background-color: white;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);" +
                    "-fx-border-color: transparent;"
            );

            // Effet hover avec couleur violette
            btn.setOnMouseEntered(e ->
                btn.setStyle(
                    "-fx-font-size: 36px;" +
                        "-fx-min-width: 60;" +
                        "-fx-min-height: 60;" +
                        "-fx-max-width: 60;" +
                        "-fx-max-height: 60;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 15;" +
                        "-fx-background-color: " + PURPLE_LIGHT + ";" +
                        "-fx-effect: dropshadow(gaussian, " + PURPLE_PRIMARY + ", 10, 0.3, 0, 3);" +
                        "-fx-border-color: " + PURPLE_PRIMARY + ";" +
                        "-fx-border-width: 2;"
                ));

            btn.setOnMouseExited(e ->
                btn.setStyle(
                    "-fx-font-size: 32px;" +
                        "-fx-min-width: 60;" +
                        "-fx-min-height: 60;" +
                        "-fx-max-width: 60;" +
                        "-fx-max-height: 60;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 15;" +
                        "-fx-background-color: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);" +
                        "-fx-border-color: transparent;"
                ));

            btn.setOnAction(e -> {
                setResult(emoji);
                close();
            });

            emojiGrid.add(btn, col, row);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        if (emojis.isEmpty()) {
            Label noResult = new Label("😕 Aucun emoji trouvé");
            noResult.setStyle(
                "-fx-padding: 30;" +
                    "-fx-text-fill: #9CA3AF;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;"
            );
            emojiGrid.add(noResult, 0, 0, 6, 1);
        }
    }
}
