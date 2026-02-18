package main;

import entities.Cours;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
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
    // CELLULE POUR LE TITRE
    // ============================================
    public static TableCell<Cours, String> titleCell() {
        return new TableCell<Cours, String>() {
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

                Cours cours = getTableView().getItems().get(getIndex());
                String oldValue = cours.getTitre();

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du titre",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    cours.setTitre(newValue);
                    try {
                        coursService.update(cours);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LA DESCRIPTION
    // ============================================
    public static TableCell<Cours, String> descriptionCell() {
        return new TableCell<Cours, String>() {
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
                textArea.setPrefWidth(300);
                textArea.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) commitEdit(textArea.getText());
                });
                setText(null);
                setGraphic(textArea);
                textArea.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() < 10 || newValue.length() > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La description doit contenir entre 10 et 1000 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Cours cours = getTableView().getItems().get(getIndex());

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification de la description",
                        "Êtes-vous sûr de vouloir modifier la description ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    cours.setDescription(newValue);
                    try {
                        coursService.update(cours);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LA DURÉE
    // ============================================
    public static TableCell<Cours, Integer> dureeCell() {
        return new TableCell<Cours, Integer>() {
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
                        setText(item + "h");
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
                if (newValue <= 0 || newValue > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La durée doit être comprise entre 1 et 1000 heures.\n" +
                                    "Valeur saisie: " + newValue + " heures.");
                    cancelEdit();
                    return;
                }

                Cours cours = getTableView().getItems().get(getIndex());
                Integer oldValue = cours.getDuree();

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification de la durée",
                        "De: " + oldValue + " heures\nVers: " + newValue + " heures",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    cours.setDuree(newValue);
                    try {
                        coursService.update(cours);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LE NIVEAU
    // ============================================
    public static TableCell<Cours, String> niveauCell() {
        return new TableCell<Cours, String>() {
            private ComboBox<String> comboBox;

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

            @Override
            public void startEdit() {
                super.startEdit();
                comboBox = new ComboBox<>();
                comboBox.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert", "Master");
                comboBox.setValue(getItem());
                comboBox.setOnAction(e -> commitEdit(comboBox.getValue()));
                comboBox.setOnKeyPressed(e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
                setText(null);
                setGraphic(comboBox);
                comboBox.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                Cours cours = getTableView().getItems().get(getIndex());
                String oldValue = cours.getNiveau();

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du niveau",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    cours.setNiveau(newValue);
                    try {
                        coursService.update(cours);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR OBLIGATOIRE
    // ============================================
    public static TableCell<Cours, Boolean> obligatoireCell() {
        return new TableCell<Cours, Boolean>() {
            private CheckBox checkBox;

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (checkBox != null) checkBox.setSelected(item);
                        setText(null);
                        setGraphic(checkBox);
                    } else {
                        HBox container = new HBox(8);
                        container.setAlignment(javafx.geometry.Pos.CENTER);
                        Circle dot = new Circle(6);
                        Label label = new Label();
                        if (item) {
                            dot.setFill(javafx.scene.paint.Color.valueOf("#10b981"));
                            label.setText("Obligatoire");
                            label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        } else {
                            dot.setFill(javafx.scene.paint.Color.valueOf("#6b7280"));
                            label.setText("Optionnel");
                            label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
                        }
                        container.getChildren().addAll(dot, label);
                        setGraphic(container);
                        setText(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                checkBox = new CheckBox("Obligatoire");
                checkBox.setSelected(getItem());
                checkBox.setOnAction(e -> commitEdit(checkBox.isSelected()));
                setGraphic(checkBox);
                setText(null);
            }

            @Override
            public void commitEdit(Boolean newValue) {
                Cours cours = getTableView().getItems().get(getIndex());
                String status = newValue ? "obligatoire" : "optionnel";

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du statut",
                        "Voulez-vous marquer ce cours comme " + status + " ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    cours.setEst_obligatoire(newValue);
                    try {
                        coursService.update(cours);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR L'IMAGE
    // ============================================
    public static TableCell<Cours, byte[]> imageCell() {
        return new TableCell<Cours, byte[]>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-cursor: hand;");
                imageView.setOnMouseClicked(e -> choisirImage());
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
                fileChooser.setTitle("Choisir une image");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
                );

                File file = fileChooser.showOpenDialog(getScene().getWindow());
                if (file != null) {
                    try {
                        byte[] newImage = Files.readAllBytes(file.toPath());
                        Cours cours = getTableView().getItems().get(getIndex());

                        boolean confirmed = AlertUtils.showConfirmation(
                                "✏️ Modification de l'image",
                                "Voulez-vous remplacer l'image du cours ?\n\n" +
                                        "Nouvelle image: " + file.getName() + "\n" +
                                        "Taille: " + (newImage.length / 1024) + " KB",
                                "Oui, remplacer",
                                "Non, annuler"
                        );

                        if (confirmed) {
                            cours.setImageCouverture(newImage);
                            coursService.update(cours);
                            getTableView().refresh();
                        }
                    } catch (Exception ex) {
                        AlertUtils.showError("❌ Erreur", "Impossible de lire l'image:\n" + ex.getMessage());
                    }
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LES COMPÉTENCES
    // ============================================
    public static TableCell<Cours, String> competencesCell() {
        return new TableCell<Cours, String>() {
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
                        setText(item != null ? item.substring(0, Math.min(20, item.length())) + "..." : "");
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textField = new TextField(getItem());
                textField.setOnAction(e -> commitEdit(textField.getText()));
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() > 500) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Les compétences ne peuvent pas dépasser 500 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Cours cours = getTableView().getItems().get(getIndex());
                cours.setCompetences_visees(newValue);
                try {
                    coursService.update(cours);
                    super.commitEdit(newValue);
                    getTableView().refresh();
                } catch (SQLException e) {
                    AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LES ACTIONS (boutons)
    // ============================================
    public static TableCell<Cours, Void> actionsCell() {
        return new TableCell<Cours, Void>() {
            private final Button btnModules = new Button("📚");
            private final Button btnLecons = new Button("📖");
            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(6, btnModules, btnLecons, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);

                btnModules.setStyle("-fx-background-color: #5E548E; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                btnLecons.setStyle("-fx-background-color: #9F86C0; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                btnModules.setOnAction(event -> {
                    Cours cours = getTableView().getItems().get(getIndex());
                    MainShellController.getInstance().showModulesViewWithCours(cours.getId(), cours.getTitre());
                });

                btnLecons.setOnAction(event -> {
                    Cours cours = getTableView().getItems().get(getIndex());
                    MainShellController.getInstance().showLeconsViewWithCours(cours.getId(), cours.getTitre());
                });

                btnDelete.setOnAction(event -> {
                    Cours cours = getTableView().getItems().get(getIndex());
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