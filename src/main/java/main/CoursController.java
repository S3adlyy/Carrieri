package main;

import entities.Cours;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import services.CoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.*;


import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;

public class CoursController {

    // FORM FIELDS - CHANGER txtDescription EN TextArea
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;  // CHANGÉ DE TextField À TextArea
    @FXML private TextField txtDuree;
    @FXML private TextField txtCompetences;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private CheckBox chkObligatoire;
    @FXML private Label lblObligatoire;
    @FXML private Label lblCount;
    @FXML private Label lblStatus;

    // SEARCH
    @FXML private TextField txtSearch;

    // TABLE
    @FXML private TableView<Cours> tableCours;
    @FXML private TableColumn<Cours, Integer> colId;
    @FXML private TableColumn<Cours, String> colTitre;
    @FXML private TableColumn<Cours, String> colDescription;
    @FXML private TableColumn<Cours, Integer> colDuree;
    @FXML private TableColumn<Cours, String> colNiveau;
    @FXML private TableColumn<Cours, String> colCompetences;
    @FXML private TableColumn<Cours, Boolean> colObligatoire;

    // BUTTONS
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnRefresh;
    @FXML private Button btnChoisirImage;
    @FXML private Label lblImageNom;
    private byte[] imageBytes;
    @FXML
    private VBox card;
    @FXML
    private ImageView imageViewForm;
    private byte[] imageBytesSelected;

    @FXML
    private TableColumn<Cours, byte[]> colImage;

    private CoursService coursServices;
    private ObservableList<Cours> coursList;
    private FilteredList<Cours> filteredList;

    private int currentUserId = 1;

    @FXML
    public void initialize() {
        coursServices = new CoursService();
        coursList = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(coursList, p -> true);

        setupNiveauComboBox();
        setupTableView();
        setupSearchListener();
        setupCheckBoxListener();
        refresh();
        btnChoisirImage.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(btnChoisirImage.getScene().getWindow());
            if (file != null) {
                try {
                    imageBytesSelected = Files.readAllBytes(file.toPath()); // ← ici
                    Image image = new Image(new FileInputStream(file));
                    imageViewForm.setImage(image); // Affiche l'image dans le formulaire
                    lblImageNom.setText(file.getName()); // Affiche le nom du fichier
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });




    }

    private void setupNiveauComboBox() {
        ObservableList<String> niveaux = FXCollections.observableArrayList(
                "Débutant",
                "Intermédiaire",
                "Avancé",
                "Expert",
                "Master"
        );
        comboNiveau.setItems(niveaux);

        // Style personnalisé pour les items
        comboNiveau.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);

                    // Style selon le niveau
                    String style = "-fx-font-weight: bold; -fx-font-size: 14px; ";
                    switch (item) {
                        case "Débutant": style += "-fx-text-fill: #10b981;"; break;
                        case "Intermédiaire": style += "-fx-text-fill: #3b82f6;"; break;
                        case "Avancé": style += "-fx-text-fill: #f59e0b;"; break;
                        case "Expert": style += "-fx-text-fill: #8b5cf6;"; break;
                        case "Master": style += "-fx-text-fill: #ef4444;"; break;
                        default: style += "-fx-text-fill: #5E548E;";
                    }
                    setStyle(style);
                }
            }
        });
    }

    private void setupTableView() {
        // Configurer les colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colCompetences.setCellValueFactory(new PropertyValueFactory<>("competences_visees"));
        colObligatoire.setCellValueFactory(new PropertyValueFactory<>("est_obligatoire"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageCouverture"));
        // Style personnalisé pour la colonne Niveau
        colNiveau.setCellFactory(column -> new TableCell<Cours, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setAlignment(javafx.geometry.Pos.CENTER);

                    // Style avec badges colorés
                    String style = "-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15; ";
                    switch (item.toLowerCase()) {
                        case "débutant":
                            style += "-fx-text-fill: #10b981; -fx-background-color: #d1fae5;";
                            break;
                        case "intermédiaire":
                            style += "-fx-text-fill: #3b82f6; -fx-background-color: #dbeafe;";
                            break;
                        case "avancé":
                            style += "-fx-text-fill: #f59e0b; -fx-background-color: #fef3c7;";
                            break;
                        case "expert":
                            style += "-fx-text-fill: #8b5cf6; -fx-background-color: #ede9fe;";
                            break;
                        case "master":
                            style += "-fx-text-fill: #ef4444; -fx-background-color: #fee2e2;";
                            break;
                        default:
                            style += "-fx-text-fill: #5E548E; -fx-background-color: #f3f4f6;";
                    }
                    setStyle(style);
                }
            }
        });

        // Style personnalisé pour la colonne Obligatoire
        colObligatoire.setCellFactory(col -> new TableCell<Cours, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox container = new HBox(8);
                    container.setAlignment(javafx.geometry.Pos.CENTER);

                    Circle dot = new Circle(6);
                    Label label = new Label();

                    if (item) {
                        dot.setFill(javafx.scene.paint.Color.valueOf("#10b981"));
                        label.setText("Obligatoire");
                        label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        dot.setFill(javafx.scene.paint.Color.valueOf("#6b7280"));
                        label.setText("Optionnel");
                        label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }

                    container.getChildren().addAll(dot, label);
                    setGraphic(container);
                }
            }
        });

        colImage.setCellFactory(column -> new TableCell<Cours, byte[]>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);    // largeur de la miniature
                imageView.setFitHeight(60);   // hauteur de la miniature
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(byte[] imageBytes, boolean empty) {
                super.updateItem(imageBytes, empty);
                if (empty || imageBytes == null) {
                    setGraphic(null);
                } else {
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    imageView.setImage(image);
                    setGraphic(imageView);
                }
            }
        });


        // Écouteur de sélection dans la table
        tableCours.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Remplir les champs texte, combo, etc.
                txtTitre.setText(newSelection.getTitre());
                txtDuree.setText(String.valueOf(newSelection.getDuree()));
                txtCompetences.setText(newSelection.getCompetences_visees());
                txtDescription.setText(newSelection.getDescription());
                comboNiveau.setValue(newSelection.getNiveau());
                chkObligatoire.setSelected(newSelection.isEst_obligatoire());

                // Afficher l'image dans un ImageView du formulaire
                if (newSelection.getImageCouverture() != null) {
                    Image image = new Image(new ByteArrayInputStream(newSelection.getImageCouverture()));
                    imageViewForm.setImage(image); // imageViewForm : un ImageView que tu ajoutes dans ton formulaire
                } else {
                    imageViewForm.setImage(null);
                }
            }
        });


        tableCours.setItems(filteredList);
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTable(newValue);
        });
    }

    private void setupCheckBoxListener() {
        chkObligatoire.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                lblObligatoire.setText("Obligatoire");
                lblObligatoire.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                lblObligatoire.setText("Optionnel");
                lblObligatoire.setStyle("-fx-text-fill: #718096;");
            }
        });
    }

    private void filterTable(String searchText) {
        filteredList.setPredicate(cours -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = searchText.toLowerCase();
            return cours.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                    cours.getNiveau().toLowerCase().contains(lowerCaseFilter) ||
                    cours.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                    cours.getCompetences_visees().toLowerCase().contains(lowerCaseFilter);
        });
        updateCount();
    }

    @FXML
    private void ajouter() {
        try {
            if (!validateForm()) {
                return;
            }

            Cours cours = new Cours(
                    txtTitre.getText(),
                    txtDescription.getText(),
                    Integer.parseInt(txtDuree.getText()),
                    comboNiveau.getValue(),
                    txtCompetences.getText(),
                    chkObligatoire.isSelected(),
                    currentUserId,
                    imageBytesSelected  // ajouter l'image ici
            );


            coursServices.ajouter(cours);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Cours ajouté avec succès !");
            refresh();
            clearForm();
            updateStatus("Nouveau cours ajouté: " + cours.getTitre());

            // Réinitialiser pour le formulaire
            imageBytesSelected = null;
            imageViewForm.setImage(null);
            lblImageNom.setText("");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "La durée doit être un nombre valide !");
        }
    }

    @FXML
    private void modifier() {
        Cours selected = tableCours.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours à modifier !");
            return;
        }

        if (selected.getCreatedBy() != currentUserId) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Vous ne pouvez pas modifier ce cours, vous n'êtes pas le créateur !");
            return;
        }

        try {
            if (!validateForm()) {
                return;
            }

            selected.setTitre(txtTitre.getText());
            selected.setDescription(txtDescription.getText());
            selected.setDuree(Integer.parseInt(txtDuree.getText()));
            selected.setNiveau(comboNiveau.getValue());
            selected.setCompetences_visees(txtCompetences.getText());
            selected.setEst_obligatoire(chkObligatoire.isSelected());

            // Si une nouvelle image a été choisie, on la met à jour
            if (imageBytesSelected != null) {
                selected.setImageCouverture(imageBytesSelected);
                imageBytesSelected = null; // Réinitialiser pour éviter de réutiliser l'ancienne image
            }

            coursServices.update(selected);

            // Mettre à jour directement la TableView
            tableCours.refresh();

            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Cours modifié avec succès !");
            clearForm();
            updateStatus("Cours modifié: " + selected.getTitre());

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "La durée doit être un nombre valide !");
        }
    }


    @FXML
    private void supprimer() {
        Cours selected = tableCours.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours à supprimer !");
            return;
        }
        if (selected.getCreatedBy() != currentUserId) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Vous ne pouvez pas supprimer ce cours, vous n'êtes pas le créateur !");
            return;
        }
        // Confirmation de suppression
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("🗑️ Confirmation");
        confirmAlert.setHeaderText("Supprimer le cours");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer le cours :\n\"" + selected.getTitre() + "\" ?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            try {
                coursServices.supprimer(selected.getId());
                showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Cours supprimé avec succès !");
                refresh();
                clearForm();
                updateStatus("Cours supprimé: " + selected.getTitre());

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
            }
        }
    }

    @FXML
    private void refresh() {
        try {
            List<Cours> list = coursServices.readByAdmin(currentUserId);
            coursList.setAll(list);
            updateCount();
            updateStatus("Liste actualisée - " + list.size() + " cours(s)");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void loadCoursToForm(Cours cours) {
        txtTitre.setText(cours.getTitre());
        txtDescription.setText(cours.getDescription());
        txtDuree.setText(String.valueOf(cours.getDuree()));
        comboNiveau.setValue(cours.getNiveau());
        txtCompetences.setText(cours.getCompetences_visees());
        chkObligatoire.setSelected(cours.isEst_obligatoire());
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtTitre.getText().trim().isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
        }
        if (comboNiveau.getValue() == null) {
            errors.append("• Le niveau est obligatoire\n");
        }
        if (txtDuree.getText().trim().isEmpty()) {
            errors.append("• La durée est obligatoire\n");
        } else {
            try {
                Integer.parseInt(txtDuree.getText());
            } catch (NumberFormatException e) {
                errors.append("• La durée doit être un nombre\n");
            }
        }

        // **Vérification obligatoire de l'image**
        if (imageBytesSelected == null) {
            errors.append("• L'image du cours est obligatoire\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Veuillez corriger :\n\n" + errors.toString());
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtTitre.clear();
        txtDescription.clear();
        txtDuree.clear();
        txtCompetences.clear();
        comboNiveau.getSelectionModel().clearSelection();
        chkObligatoire.setSelected(false);
        tableCours.getSelectionModel().clearSelection();
    }

    private void updateCount() {
        int total = coursList.size();
        int filtered = filteredList.size();
        int obligatoires = (int) coursList.stream().filter(Cours::isEst_obligatoire).count();

        if (total == filtered) {
            lblCount.setText(total + " cours(s) • " + obligatoires + " obligatoire(s)");
        } else {
            lblCount.setText(filtered + "/" + total + " cours(s) • " + obligatoires + " obligatoire(s)");
        }
    }

    private void updateStatus(String message) {
        lblStatus.setText("📌 " + message);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);

        // Style personnalisé selon le type
        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            // Si le CSS n'existe pas, on continue sans
        }

        alert.showAndWait();
    }
}