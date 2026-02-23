package main;

import entities.Cours;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import main.MainShellController;
import utils.AlertUtils;
import services.CoursService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;

public class CoursCell {

    private static final CoursService coursService = new CoursService();

    // ============================================
    // CELLULE TITRE
    // ============================================
    public static TableCell<Cours, String> getTitleCell() {
        return new CoursTitleCellImpl();
    }

    private static class CoursTitleCellImpl extends TableCell<Cours, String> {
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
                    // ✅ CORRIGÉ : Utilise la nouvelle alerte stylisée
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le titre doit contenir entre 3 et 200 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                // ✅ CORRIGÉ : Utilise la nouvelle confirmation stylisée
                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setTitre(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        // ✅ Optionnel : message de succès
                        AlertUtils.showSuccess("✅ Succès", "Titre modifié avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE DESCRIPTION
    // ============================================
    public static TableCell<Cours, String> getDescriptionCell() {
        return new CoursDescriptionCellImpl();
    }

    private static class CoursDescriptionCellImpl extends TableCell<Cours, String> {
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
            setText(getItem() != null ? getItem().substring(0, Math.min(40, getItem().length())) + "..." : "");
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
                    setText(item != null ? item.substring(0, Math.min(40, item.length())) + "..." : "");
                    setGraphic(null);
                }
            }
        }

        private void createTextArea() {
            textArea = new TextArea(getItem());
            textArea.setWrapText(true);
            textArea.setPrefRowCount(3);
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

                if (newValue.length() < 10 || newValue.length() > 1000) {
                    // ✅ CORRIGÉ
                    AlertUtils.showWarning("⚠️ Validation",
                            "La description doit contenir entre 10 et 1000 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                // ✅ CORRIGÉ
                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Êtes-vous sûr de vouloir modifier la description ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setDescription(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Description modifiée avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE DURÉE
    // ============================================
    public static TableCell<Cours, Integer> getDureeCell() {
        return new CoursDureeCellImpl();
    }

    private static class CoursDureeCellImpl extends TableCell<Cours, Integer> {
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
                    setText(item != null ? item.toString() : "");
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

                if (newValue <= 0 || newValue > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La durée doit être comprise entre 1 et 1000 heures.\n\n" +
                                    "Valeur saisie: " + newValue + " heures.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: " + oldValue + " heures\nVers: " + newValue + " heures",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setDuree(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Durée modifiée avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }

        @Override
        public void commitEdit(Integer newValue) {
            // Cette méthode est appelée par la superclasse
            super.commitEdit(newValue);
        }
    }

    // ============================================
    // CELLULE NIVEAU
    // ============================================
    public static TableCell<Cours, String> getNiveauCell() {
        return new CoursNiveauCellImpl();
    }

    private static class CoursNiveauCellImpl extends TableCell<Cours, String> {
        private ComboBox<String> comboBox;
        private boolean confirming;

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            createComboBox();
            setText(null);
            setGraphic(comboBox);
            comboBox.requestFocus();
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
                    if (comboBox != null) comboBox.setValue(item);
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        }

        private void createComboBox() {
            comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert", "Master");
            comboBox.setValue(getItem());
            comboBox.setOnAction(e -> confirmEdit(comboBox.getValue()));
            comboBox.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private void confirmEdit(String newValue) {
            if (confirming) return;
            confirming = true;
            try {
                String oldValue = getItem();

                if (newValue.equals(oldValue)) {
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
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setNiveau(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Niveau modifié avec succès.");
                    } catch (SQLException ex) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + ex.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE OBLIGATOIRE
    // ============================================
    public static TableCell<Cours, Boolean> getObligatoireCell() {
        return new CoursObligatoireCellImpl();
    }

    private static class CoursObligatoireCellImpl extends TableCell<Cours, Boolean> {
        private CheckBox checkBox;
        private boolean confirming;

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            createCheckBox();
            setText(null);
            setGraphic(checkBox);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateDisplay(getItem());
        }

        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (checkBox != null) checkBox.setSelected(item != null && item);
                    setText(null);
                    setGraphic(checkBox);
                } else {
                    updateDisplay(item);
                }
            }
        }

        private void updateDisplay(Boolean item) {
            HBox container = new HBox(8);
            container.setAlignment(javafx.geometry.Pos.CENTER);
            Circle dot = new Circle(6);
            Label label = new Label();
            if (item != null && item) {
                dot.setFill(javafx.scene.paint.Color.valueOf("#10b981"));
                label.setText("Obligatoire");
                label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                dot.setFill(javafx.scene.paint.Color.valueOf("#6b7280"));
                label.setText("Optionnel");
                label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
            }
            container.getChildren().addAll(dot, label);
            setText(null);
            setGraphic(container);
        }

        private void createCheckBox() {
            checkBox = new CheckBox("Obligatoire");
            checkBox.setSelected(getItem() != null && getItem());
            checkBox.setOnAction(e -> confirmEdit(checkBox.isSelected()));
        }

        private void confirmEdit(boolean newValue) {
            if (confirming) return;
            confirming = true;
            try {
                boolean oldValue = getItem();

                if (newValue == oldValue) {
                    cancelEdit();
                    return;
                }

                String status = newValue ? "obligatoire" : "optionnel";
                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Voulez-vous marquer ce cours comme " + status + " ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setEst_obligatoire(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Statut modifié avec succès.");
                    } catch (SQLException ex) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + ex.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE IMAGE
    // ============================================
    public static TableCell<Cours, byte[]> getImageCell() {
        return new CoursImageCellImpl();
    }

    private static class CoursImageCellImpl extends TableCell<Cours, byte[]> {
        private final ImageView imageView = new ImageView();
        private boolean confirming;

        {
            imageView.setFitWidth(80);
            imageView.setFitHeight(60);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-cursor: hand;");
            imageView.setOnMouseClicked(e -> choisirImage());
        }

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
            super.startEdit();
            choisirImage();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateItem(getItem(), false);
        }

        @Override
        protected void updateItem(byte[] imageBytes, boolean empty) {
            super.updateItem(imageBytes, empty);
            if (empty || imageBytes == null) {
                setGraphic(null);
            } else {
                imageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                setGraphic(imageView);
            }
        }

        private void choisirImage() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une nouvelle image");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(getTableView().getScene().getWindow());

            if (file != null) {
                try {
                    byte[] newImageBytes = Files.readAllBytes(file.toPath());
                    confirmEdit(newImageBytes, file.getName());
                } catch (Exception ex) {
                    AlertUtils.showError("❌ Erreur", "Impossible de lire l'image:\n\n" + ex.getMessage());
                    cancelEdit();
                }
            } else {
                cancelEdit();
            }
        }

        private void confirmEdit(byte[] newImageBytes, String fileName) {
            if (confirming) return;
            confirming = true;
            try {
                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Voulez-vous remplacer l'image de ce cours ?\n\n" +
                                "Nouvelle image: " + fileName + "\n" +
                                "Taille: " + (newImageBytes.length / 1024) + " KB",
                        "Oui, remplacer",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setImageCouverture(newImageBytes);
                    try {
                        coursService.update(cours);
                        commitEdit(newImageBytes);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Image modifiée avec succès.");
                    } catch (SQLException ex) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + ex.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE COMPÉTENCES
    // ============================================
    public static TableCell<Cours, String> getCompetencesCell() {
        return new CoursCompetencesCellImpl();
    }

    private static class CoursCompetencesCellImpl extends TableCell<Cours, String> {
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
            setText(getItem() != null ? getItem().substring(0, Math.min(20, getItem().length())) + "" : "");
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
                    setText(item != null ? item.substring(0, Math.min(20, item.length())) + "" : "");
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

                if (!newValue.isEmpty() && newValue.length() > 500) {
                    AlertUtils.showWarning("⚠ Validation",
                            "Les compétences ne peuvent pas dépasser 500 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Êtes-vous sûr de modifier les compétences ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setCompetences_visees(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Compétences modifiées avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }
    // ============================================
// CELLULE PRIX (NOUVEAU)
// ============================================
    public static TableCell<Cours, Double> getPrixCell() {
        return new CoursPrixCellImpl();
    }

    private static class CoursPrixCellImpl extends TableCell<Cours, Double> {
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
            updateDisplay(getItem());
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) textField.setText(item != null ? String.format("%.2f", item) : "0.00");
                    setText(null);
                    setGraphic(textField);
                } else {
                    updateDisplay(item);
                }
            }
        }

        private void updateDisplay(Double item) {
            HBox container = new HBox(8);
            container.setAlignment(Pos.CENTER);

            Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
            boolean estPayant = cours != null && cours.isEstPayant();

            Label prixLabel = new Label();
            if (estPayant && item != null && item > 0) {
                prixLabel.setText(String.format("%.2f TND", item));
                prixLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
                Label euroIcon = new Label("💰");
                euroIcon.setStyle("-fx-font-size: 12px;");
                container.getChildren().addAll(euroIcon, prixLabel);
            } else {
                prixLabel.setText("Gratuit");
                prixLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
                Label freeIcon = new Label("🎁");
                freeIcon.setStyle("-fx-font-size: 12px;");
                container.getChildren().addAll(freeIcon, prixLabel);
            }

            setText(null);
            setGraphic(container);
        }

        private void createTextField() {
            Double currentValue = getItem();
            textField = new TextField(currentValue != null ? String.format("%.2f", currentValue) : "0.00");

            // Validation pour n'accepter que les nombres décimaux
            textField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                    textField.setText(old);
                }
            });

            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    try {
                        confirmEdit(Double.parseDouble(textField.getText()));
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarning("⚠ Validation", "Veuillez entrer un prix valide.");
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    try {
                        confirmEdit(Double.parseDouble(textField.getText()));
                    } catch (NumberFormatException ex) {
                        cancelEdit();
                    }
                }
            });
        }

        private void confirmEdit(double newValue) {
            if (confirming) return;
            confirming = true;
            try {
                Double oldValue = getItem();

                if (Math.abs(newValue - (oldValue != null ? oldValue : 0.0)) < 0.01) {
                    cancelEdit();
                    return;
                }

                if (newValue < 0) {
                    AlertUtils.showWarning("⚠ Validation", "Le prix ne peut pas être négatif.");
                    cancelEdit();
                    return;
                }

                if (newValue > 10000) {
                    AlertUtils.showWarning("⚠ Validation", "Le prix ne peut pas dépasser 1000 TND.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: " + (oldValue != null ? String.format("%.2f TND", oldValue) : "0.00 TND") +
                                "\nVers: " + String.format("%.2f TND", newValue),
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setPrix(newValue);
                    cours.setEstPayant(newValue > 0);

                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Prix modifié avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }
}