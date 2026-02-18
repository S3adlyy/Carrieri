package main;

import entities.Lecon;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import utils.AlertUtils;
import services.LeconService;

import java.io.File;
import java.nio.file.Files;

public class LeconCell {

    private static final LeconService leconService = new LeconService();

    // ============================================
    // CELLULE POUR LE TITRE
    // ============================================
    public static TableCell<Lecon, String> titleCell() {
        return new TableCell<Lecon, String>() {
            private TextField textField;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) textField.setText(item);
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textField = new TextField(getItem());
                textField.setOnAction(e -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) commitEdit(textField.getText());
                });
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() < 3 || newValue.length() > 200) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le titre doit contenir entre 3 et 200 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Lecon lecon = getTableView().getItems().get(getIndex());
                String oldValue = lecon.getTitre();

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du titre",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    lecon.setTitre(newValue);
                    try {
                        leconService.modifier(lecon);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LE CONTENU
    // ============================================
    public static TableCell<Lecon, String> contenuCell() {
        return new TableCell<Lecon, String>() {
            private TextArea textArea;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textArea != null) textArea.setText(item);
                        setText(null);
                        setGraphic(textArea);
                    } else {
                        setText(item != null ? item.substring(0, Math.min(30, item.length())) + "..." : "");
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textArea = new TextArea(getItem());
                textArea.setWrapText(true);
                textArea.setPrefRowCount(4);
                textArea.setPrefWidth(400);
                textArea.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) commitEdit(textArea.getText());
                });
                setText(null);
                setGraphic(textArea);
                textArea.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() < 500 || newValue.length() > 10000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le contenu doit contenir entre 500 et 10000 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Lecon lecon = getTableView().getItems().get(getIndex());

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du contenu",
                        "Êtes-vous sûr de vouloir modifier le contenu de cette leçon ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    lecon.setContenu(newValue);
                    try {
                        leconService.modifier(lecon);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR L'ORDRE
    // ============================================
    public static TableCell<Lecon, Integer> ordreCell(ObservableList<Lecon> leconList) {
        return new TableCell<Lecon, Integer>() {
            private TextField textField;

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) textField.setText(item.toString());
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(item.toString());
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textField = new TextField(getItem().toString());
                textField.textProperty().addListener((obs, old, newVal) -> {
                    if (!newVal.matches("\\d*")) {
                        textField.setText(newVal.replaceAll("[^\\d]", ""));
                    }
                });
                textField.setOnAction(e -> {
                    try {
                        commitEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarning("⚠️ Validation", "Veuillez entrer un nombre valide.");
                    }
                });
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void commitEdit(Integer newValue) {
                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "L'ordre doit être compris entre 1 et 100.\n" +
                                    "Valeur saisie: " + newValue);
                    cancelEdit();
                    return;
                }

                Lecon lecon = getTableView().getItems().get(getIndex());
                Integer oldValue = lecon.getOrdre();

                boolean ordreExiste = leconList.stream()
                        .anyMatch(l -> l.getOrdre() == newValue &&
                                l.getModuleId() == lecon.getModuleId() &&
                                l.getId() != lecon.getId());

                if (ordreExiste) {
                    AlertUtils.showWarning("⚠️ Ordre déjà utilisé",
                            "Une autre leçon a déjà l'ordre " + newValue + " dans ce module.\n" +
                                    "Veuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification de l'ordre",
                        "De: " + oldValue + "\nVers: " + newValue,
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    lecon.setOrdre(newValue);
                    try {
                        leconService.modifier(lecon);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LA VIDÉO
    // ============================================
    public static TableCell<Lecon, byte[]> videoCell() {
        return new TableCell<Lecon, byte[]>() {

            @Override
            protected void updateItem(byte[] videoBytes, boolean empty) {
                super.updateItem(videoBytes, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (videoBytes != null && videoBytes.length > 0) {
                        setText("🎥 " + formatTaille(videoBytes.length));
                        setStyle("-fx-cursor: hand;");
                        setOnMouseClicked(e -> choisirVideo());
                    } else {
                        setText("❌ Ajouter");
                        setStyle("-fx-cursor: hand; -fx-text-fill: #5E548E; -fx-font-weight: bold;");
                        setOnMouseClicked(e -> choisirVideo());
                    }
                }
            }

            private String formatTaille(long taille) {
                if (taille < 1024) return taille + " B";
                if (taille < 1024 * 1024) return (taille / 1024) + " KB";
                return String.format("%.1f MB", taille / (1024.0 * 1024.0));
            }

            private void choisirVideo() {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choisir une vidéo");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Vidéos MP4", "*.mp4"),
                        new FileChooser.ExtensionFilter("Tous les formats", "*.mp4", "*.avi", "*.mov", "*.mkv")
                );

                File file = fileChooser.showOpenDialog(getScene().getWindow());
                if (file != null) {
                    try {
                        byte[] newVideo = Files.readAllBytes(file.toPath());

                        if (newVideo.length > 64 * 1024 * 1024) {
                            AlertUtils.showWarning("⚠️ Fichier trop volumineux",
                                    "La vidéo ne peut pas dépasser 64 MB.\n" +
                                            "Taille: " + formatTaille(newVideo.length));
                            return;
                        }

                        Lecon lecon = getTableView().getItems().get(getIndex());

                        boolean confirmed = AlertUtils.showConfirmation(
                                "✏️ Modification de la vidéo",
                                "Voulez-vous " + (lecon.getVideo() == null ? "ajouter" : "remplacer") + " la vidéo ?\n\n" +
                                        "Fichier: " + file.getName() + "\n" +
                                        "Taille: " + formatTaille(newVideo.length),
                                "Oui",
                                "Non"
                        );

                        if (confirmed) {
                            lecon.setVideo(newVideo);
                            leconService.modifier(lecon);
                            getTableView().refresh();
                        }
                    } catch (Exception ex) {
                        AlertUtils.showError("❌ Erreur", "Impossible de lire la vidéo:\n" + ex.getMessage());
                    }
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LES ACTIONS
    // ============================================
    public static TableCell<Lecon, Void> actionsCell() {
        return new TableCell<Lecon, Void>() {
            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(5, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                btnDelete.setOnAction(event -> {
                    Lecon lecon = getTableView().getItems().get(getIndex());
                    // La logique de suppression sera dans le contrôleur
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        };
    }
}