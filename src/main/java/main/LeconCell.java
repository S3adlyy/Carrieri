package main;

import entities.Lecon;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import utils.AlertUtils;
import services.LeconService;

import java.io.File;
import java.nio.file.Files;

public class LeconCell {

    private static final LeconService leconService = new LeconService();

    // ============================================
    // CELLULE TITRE
    // ============================================
    public static TableCell<Lecon, String> getTitleCell() {
        return new LeconTitleCellImpl();
    }

    private static class LeconTitleCellImpl extends TableCell<Lecon, String> {
        private TextField textField;
        private boolean confirming;

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

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

        private void createTextField() {
            textField = new TextField(getItem());
            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    confirmEdit();
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) confirmEdit();
            });
        }

        private void confirmEdit() {
            if (confirming) return;
            confirming = true;
            try {
                String newValue = textField.getText().trim();
                String oldValue = getItem();

                if (newValue.equals(oldValue)) {
                    cancelEdit();
                    return;
                }

                if (newValue.length() < 3 || newValue.length() > 200) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le titre de la leçon doit contenir entre 3 et 200 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                    if (lecon == null) {
                        cancelEdit();
                        return;
                    }
                    lecon.setTitre(newValue);
                    try {
                        leconService.modifier(lecon);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Titre de la leçon modifié avec succès.");
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE CONTENU
    // ============================================
    public static TableCell<Lecon, String> getContenuCell() {
        return new LeconContenuCellImpl();
    }

    private static class LeconContenuCellImpl extends TableCell<Lecon, String> {
        private TextArea textArea;
        private boolean confirming;

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            createTextArea();
            setText(null);
            setGraphic(textArea);
            textArea.selectAll();
            textArea.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem().substring(0, Math.min(20, getItem().length())) + "..." : "");
            setGraphic(null);
        }

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
                    setText(item != null ? item.substring(0, Math.min(20, item.length())) + "..." : "");
                    setGraphic(null);
                }
            }
        }

        private void createTextArea() {
            textArea = new TextArea(getItem());
            textArea.setWrapText(true);
            textArea.setPrefRowCount(4);
            textArea.setPrefWidth(400);
            textArea.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
            textArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) confirmEdit();
            });
        }

        private void confirmEdit() {
            if (confirming) return;
            confirming = true;
            try {
                String newValue = textArea.getText().trim();
                String oldValue = getItem();

                if (newValue.equals(oldValue)) {
                    cancelEdit();
                    return;
                }

                if (newValue.length() < 500 || newValue.length() > 10000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le contenu doit contenir entre 500 et 10000 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Êtes-vous sûr de vouloir modifier le contenu de la leçon ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                    if (lecon == null) {
                        cancelEdit();
                        return;
                    }
                    lecon.setContenu(newValue);
                    try {
                        leconService.modifier(lecon);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Contenu de la leçon modifié avec succès.");
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE ORDRE
    // ============================================
    public static TableCell<Lecon, Integer> getOrdreCell(ObservableList<Lecon> leconList) {
        return new LeconOrdreCellImpl(leconList);
    }

    private static class LeconOrdreCellImpl extends TableCell<Lecon, Integer> {
        private final ObservableList<Lecon> leconList;
        private TextField textField;
        private boolean confirming;

        public LeconOrdreCellImpl(ObservableList<Lecon> leconList) {
            this.leconList = leconList;
        }

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem().toString() : "");
            setGraphic(null);
        }

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

        private void createTextField() {
            textField = new TextField(getItem() != null ? getItem().toString() : "");
            textField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    textField.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    try {
                        confirmEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarning("⚠️ Validation", "Veuillez entrer un nombre valide.");
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    try {
                        confirmEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        cancelEdit();
                    }
                }
            });
        }

        private void confirmEdit(int newValue) {
            if (confirming) return;
            confirming = true;
            try {
                Integer oldValue = getItem();

                if (newValue == oldValue) {
                    cancelEdit();
                    return;
                }

                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "L'ordre doit être compris entre 1 et 100.\n\n" +
                                    "Valeur saisie: " + newValue);
                    cancelEdit();
                    return;
                }

                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    cancelEdit();
                    return;
                }

                boolean ordreExiste = leconList.stream()
                        .anyMatch(l -> l.getOrdre() == newValue &&
                                l.getModuleId() == lecon.getModuleId() &&
                                l.getId() != lecon.getId());

                if (ordreExiste) {
                    AlertUtils.showWarning("⚠️ Ordre déjà utilisé",
                            "Une autre leçon a déjà l'ordre " + newValue + " dans ce module.\n\n" +
                                    "Veuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: " + oldValue + "\nVers: " + newValue,
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    lecon.setOrdre(newValue);
                    try {
                        leconService.modifier(lecon);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Ordre de la leçon modifié avec succès.");
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE VIDÉO
    // ============================================
    public static TableCell<Lecon, byte[]> getVideoCell() {
        return new LeconVideoCellImpl();
    }

    private static class LeconVideoCellImpl extends TableCell<Lecon, byte[]> {
        private boolean confirming;

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
                                "La vidéo ne peut pas dépasser 64 MB.\n\n" +
                                        "Taille: " + formatTaille(newVideo.length));
                        return;
                    }

                    confirmEdit(newVideo, file.getName());
                } catch (Exception ex) {
                    AlertUtils.showError("❌ Erreur", "Impossible de lire la vidéo:\n\n" + ex.getMessage());
                }
            }
        }

        private void confirmEdit(byte[] newVideoBytes, String fileName) {
            if (confirming) return;
            confirming = true;
            try {
                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    return;
                }

                String action = lecon.getVideo() == null ? "Ajouter" : "Remplacer";
                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        action + " la vidéo ?\n\n" +
                                "Fichier: " + fileName + "\n" +
                                "Taille: " + formatTaille(newVideoBytes.length),
                        "Oui",
                        "Non"
                );

                if (confirmed) {
                    lecon.setVideo(newVideoBytes);
                    leconService.modifier(lecon);
                    commitEdit(newVideoBytes);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "Vidéo " + (lecon.getVideo() == null ? "ajoutée" : "remplacée") + " avec succès.");
                }
            } catch (Exception e) {
                AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n\n" + e.getMessage());
            } finally {
                confirming = false;
            }
        }
    }
}