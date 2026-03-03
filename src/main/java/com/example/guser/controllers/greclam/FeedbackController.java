package com.example.guser.controllers.greclam;

import entities.greclam.Feedback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import services.greclam.FeedbackService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class FeedbackController implements Initializable {

    @FXML private TableView<Feedback> feedbackTable;
    @FXML private TextField minNoteField;
    @FXML private TextField maxNoteField;
    @FXML private Label averageLabel;

    private ObservableList<Feedback> feedbackList = FXCollections.observableArrayList();
    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupNoteFields();
        loadFeedback();

        // Forcer la table à utiliser toute la largeur disponible
        feedbackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupTableColumns() {
        // Effacer les colonnes existantes pour éviter les doublons
        feedbackTable.getColumns().clear();

        // Colonne ID (non éditable)
        TableColumn<Feedback, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        idCol.setMaxWidth(50);
        idCol.setMinWidth(50);

        // Colonne Commentaire (éditable)
        TableColumn<Feedback, String> commentaireCol = new TableColumn<>("Commentaire");
        commentaireCol.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        commentaireCol.setPrefWidth(300);
        commentaireCol.setCellFactory(TextFieldTableCell.forTableColumn());
        commentaireCol.setOnEditCommit(event -> {
            Feedback feedback = event.getRowValue();
            feedback.setCommentaire(event.getNewValue());
            updateFeedback(feedback);
        });

        // Colonne Note (éditable)
        TableColumn<Feedback, Integer> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(60);
        noteCol.setMaxWidth(60);
        noteCol.setMinWidth(60);

        // Configuration spéciale pour la note (édition + couleur)
        noteCol.setCellFactory(column -> new TableCell<Feedback, Integer>() {
            private final TextField textField = new TextField();

            {
                // Action quand on appuie sur Entrée
                textField.setOnAction(event -> {
                    try {
                        int newValue = Integer.parseInt(textField.getText());
                        if (newValue >= 0 && newValue <= 100) {
                            commitEdit(newValue);
                        } else {
                            cancelEdit();
                            showAlert(Alert.AlertType.WARNING, "Validation", "Note invalide",
                                    "La note doit être comprise entre 0 et 100.");
                        }
                    } catch (NumberFormatException e) {
                        cancelEdit();
                        showAlert(Alert.AlertType.WARNING, "Validation", "Format invalide",
                                "Veuillez entrer un nombre valide.");
                    }
                });

                // Perte de focus = annuler
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        cancelEdit();
                    }
                });
            }

            @Override
            protected void updateItem(Integer note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(note.toString());
                    setGraphic(null);

                    // Colorer le texte selon la valeur
                    if (note >= 80) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (note >= 60) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (note >= 40) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                if (getItem() != null) {
                    textField.setText(getItem().toString());
                    setText(null);
                    setGraphic(textField);
                    textField.requestFocus();
                    textField.selectAll();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() == null ? "" : getItem().toString());
                setGraphic(null);
            }
        });

        noteCol.setOnEditCommit(event -> {
            Feedback feedback = event.getRowValue();
            feedback.setNote(event.getNewValue());
            updateFeedback(feedback);
        });

        // Colonne Date (non éditable)
        TableColumn<Feedback, Date> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateCol.setPrefWidth(120);
        dateCol.setMaxWidth(120);
        dateCol.setMinWidth(120);
        dateCol.setCellFactory(column -> new TableCell<Feedback, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(format.format(date));
                }
            }
        });

        /*// Colonne Rendu ID (éditable)
        TableColumn<Feedback, Integer> renduIdCol = new TableColumn<>("Rendu ID");
        renduIdCol.setCellValueFactory(new PropertyValueFactory<>("renduId"));
        renduIdCol.setPrefWidth(80);
        renduIdCol.setMaxWidth(80);
        renduIdCol.setMinWidth(80);
        renduIdCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        renduIdCol.setOnEditCommit(event -> {
            Feedback feedback = event.getRowValue();
            feedback.setRenduId(event.getNewValue());
            updateFeedback(feedback);
        });*/

        // Colonne Actions (uniquement bouton Supprimer)
        TableColumn<Feedback, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(50);
        actionCol.setMaxWidth(50);
        actionCol.setMinWidth(50);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
                deleteBtn.setTooltip(new Tooltip("Supprimer"));

                deleteBtn.setOnAction(event -> {
                    Feedback feedback = getTableView().getItems().get(getIndex());
                    deleteFeedback(feedback);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        // Ajouter toutes les colonnes
        feedbackTable.getColumns().addAll(idCol, commentaireCol, noteCol, dateCol, actionCol);

        // Activer l'édition
        feedbackTable.setEditable(true);
        feedbackTable.setItems(feedbackList);
    }

    private void updateFeedback(Feedback feedback) {
        try {
            feedbackService.update(feedback);
            System.out.println("✅ Feedback #" + feedback.getId() + " mis à jour");
            updateAverage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de mise à jour", e.getMessage());
            loadFeedback();
        }
    }

    private void setupNoteFields() {
        minNoteField.setText("0");
        maxNoteField.setText("100");

        minNoteField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minNoteField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        maxNoteField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                maxNoteField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void loadFeedback() {
        try {
            feedbackList.clear();
            feedbackList.addAll(feedbackService.read());
            updateAverage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger les feedbacks: " + e.getMessage());
        }
    }

    @FXML
    private void showAddFeedbackForm() {
        openFeedbackForm(null);
    }

    @FXML
    private void filterByNoteRange() {
        try {
            int minNote = minNoteField.getText().isEmpty() ? 0 : Integer.parseInt(minNoteField.getText());
            int maxNote = maxNoteField.getText().isEmpty() ? 100 : Integer.parseInt(maxNoteField.getText());

            if (minNote > maxNote) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Plage invalide",
                        "La note minimale ne peut pas être supérieure à la note maximale.");
                return;
            }

            feedbackList.setAll(feedbackService.getByNoteRange(minNote, maxNote));
            updateAverage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format invalide",
                    "Veuillez entrer des nombres valides pour les notes.");
        }
    }

    @FXML
    private void deleteFeedback() {
        Feedback selected = feedbackTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deleteFeedback(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucune sélection",
                    "Veuillez sélectionner un feedback à supprimer.");
        }
    }

    private void deleteFeedback(Feedback feedback) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le feedback");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce feedback ?\n\n" +
                "Commentaire: " + feedback.getCommentaire());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                feedbackService.supprimer(feedback.getId());
                feedbackList.remove(feedback);
                updateAverage();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback supprimé",
                        "Le feedback a été supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", e.getMessage());
            }
        }
    }

    private void openFeedbackForm(Feedback feedback) {
        try {
            // Chemin corrigé avec le package complet
            String fxmlPath = "/com/example/guser/greclam/feedbackForm.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                // Essayer d'autres chemins
                fxmlUrl = getClass().getResource("/feedbackForm.fxml");
            }

            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier introuvable",
                        "feedbackForm.fxml n'a pas été trouvé.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            FeedbackFormController controller = loader.getController();
            controller.setFeedback(feedback);
            controller.setFeedbackList(feedbackList);

            // Créer une nouvelle fenêtre (Stage) au lieu d'un Dialog
            Stage stage = new Stage();
            stage.setTitle(feedback == null ? "Ajouter un feedback" : "Modifier le feedback");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloque la fenêtre parente
            stage.setResizable(false);

            // Attendre la fermeture de la fenêtre
            stage.showAndWait();

            // Recharger la liste après fermeture
            loadFeedback();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'interface",
                    "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateAverage() {
        try {
            double average = feedbackList.stream()
                    .mapToInt(Feedback::getNote)
                    .average()
                    .orElse(0.0);

            String color;
            if (average >= 80) color = "#27ae60";
            else if (average >= 60) color = "#f39c12";
            else if (average >= 40) color = "#e67e22";
            else color = "#e74c3c";

            averageLabel.setText(String.format("Moyenne: %.1f/100", average));
            averageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
        } catch (Exception e) {
            averageLabel.setText("Moyenne: N/A");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}