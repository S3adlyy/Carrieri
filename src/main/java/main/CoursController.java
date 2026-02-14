package main;

import entities.Cours;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.QuizAutoGenerator;
import utils.MyDatabase;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CoursController {

    // FORM FIELDS
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtDuree;
    @FXML private TextField txtCompetences;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private CheckBox chkObligatoire;
    @FXML private Label lblObligatoire;
    @FXML private Label lblCount;
    @FXML private Label lblStatus;
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
    @FXML private TableColumn<Cours, byte[]> colImage;

    // BUTTONS
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnRefresh;
    @FXML private Button btnChoisirImage;
    @FXML private Label lblImageNom;
    @FXML private ImageView imageViewForm;
    @FXML private VBox card;

    private CoursService coursService;
    private ObservableList<Cours> coursList;
    private FilteredList<Cours> filteredList;

    private int currentUserId = 1;
    private byte[] imageBytesSelected;
    private Cours coursSelectionne = null;

    @FXML
    public void initialize() {
        coursService = new CoursService();
        coursList = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(coursList, p -> true);

        setupNiveauComboBox();
        setupTableView();
        setupSearchListener();
        setupCheckBoxListener();
        setupImageChooser();

        refresh();
    }

    private void setupImageChooser() {
        btnChoisirImage.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(btnChoisirImage.getScene().getWindow());
            if (file != null) {
                try {
                    imageBytesSelected = Files.readAllBytes(file.toPath());
                    Image image = new Image(new FileInputStream(file));
                    imageViewForm.setImage(image);
                    lblImageNom.setText(file.getName());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void setupNiveauComboBox() {
        ObservableList<String> niveaux = FXCollections.observableArrayList(
                "Débutant", "Intermédiaire", "Avancé", "Expert", "Master"
        );
        comboNiveau.setItems(niveaux);
    }

    private void setupTableView() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colCompetences.setCellValueFactory(new PropertyValueFactory<>("competences_visees"));
        colObligatoire.setCellValueFactory(new PropertyValueFactory<>("est_obligatoire"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageCouverture"));

        // Style pour le niveau
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
                    String style = "-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15; ";
                    switch (item.toLowerCase()) {
                        case "débutant": style += "-fx-text-fill: #10b981; -fx-background-color: #d1fae5;"; break;
                        case "intermédiaire": style += "-fx-text-fill: #3b82f6; -fx-background-color: #dbeafe;"; break;
                        case "avancé": style += "-fx-text-fill: #f59e0b; -fx-background-color: #fef3c7;"; break;
                        case "expert": style += "-fx-text-fill: #8b5cf6; -fx-background-color: #ede9fe;"; break;
                        case "master": style += "-fx-text-fill: #ef4444; -fx-background-color: #fee2e2;"; break;
                    }
                    setStyle(style);
                }
            }
        });

        // Style pour obligatoire
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
                        label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else {
                        dot.setFill(javafx.scene.paint.Color.valueOf("#6b7280"));
                        label.setText("Optionnel");
                        label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
                    }
                    container.getChildren().addAll(dot, label);
                    setGraphic(container);
                }
            }
        });

        // Image dans la table
        colImage.setCellFactory(column -> new TableCell<Cours, byte[]>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
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
        });

        // Sélection dans la table
        tableCours.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                coursSelectionne = newSelection;
                chargerCoursFormulaire(newSelection);
            }
        });

        tableCours.setItems(filteredList);
    }

    private void chargerCoursFormulaire(Cours cours) {
        txtTitre.setText(cours.getTitre());
        txtDuree.setText(String.valueOf(cours.getDuree()));
        txtCompetences.setText(cours.getCompetences_visees());
        txtDescription.setText(cours.getDescription());
        comboNiveau.setValue(cours.getNiveau());
        chkObligatoire.setSelected(cours.isEst_obligatoire());

        if (cours.getImageCouverture() != null) {
            imageViewForm.setImage(new Image(new ByteArrayInputStream(cours.getImageCouverture())));
            imageBytesSelected = cours.getImageCouverture();
            lblImageNom.setText("Image chargée");
        }
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable(newValue));
    }

    private void setupCheckBoxListener() {
        chkObligatoire.selectedProperty().addListener((observable, oldValue, newValue) -> {
            lblObligatoire.setText(newValue ? "Obligatoire" : "Optionnel");
            lblObligatoire.setStyle(newValue ? "-fx-text-fill: #10b981; -fx-font-weight: bold;" : "-fx-text-fill: #718096;");
        });
    }

    private void filterTable(String searchText) {
        filteredList.setPredicate(cours -> {
            if (searchText == null || searchText.isEmpty()) return true;
            String lowerCaseFilter = searchText.toLowerCase();
            return cours.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                    cours.getNiveau().toLowerCase().contains(lowerCaseFilter);
        });
        updateCount();
    }

    // ============================================
    // MÉTHODE PRINCIPALE : AJOUTER COURS + CONTENU
    // ============================================
    @FXML
    private void ajouter() {
        try {
            if (!validateForm()) return;

            // 1. Créer le cours
            Cours nouveauCours = new Cours(
                    txtTitre.getText(),
                    txtDescription.getText(),
                    Integer.parseInt(txtDuree.getText()),
                    comboNiveau.getValue(),
                    txtCompetences.getText(),
                    chkObligatoire.isSelected(),
                    currentUserId,
                    imageBytesSelected
            );

            // 2. Sauvegarder le cours
            coursService.ajouter(nouveauCours);

            // 3. Récupérer le cours avec son ID
            List<Cours> tousCours = coursService.readByAdmin(currentUserId);
            Optional<Cours> coursAjouteOpt = tousCours.stream()
                    .filter(c -> c.getTitre().equals(nouveauCours.getTitre()))
                    .reduce((first, second) -> second);

            if (coursAjouteOpt.isPresent()) {
                Cours coursAjoute = coursAjouteOpt.get();
                // 4. Ouvrir la fenêtre pour ajouter modules et leçons
                ouvrirAjoutContenu(coursAjoute);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
        }
    }

    // ============================================
    // OUVRIR LA FENÊTRE D'AJOUT DE CONTENU
    // ============================================
    private void ouvrirAjoutContenu(Cours cours) {
        try {
            // Charger le ModuleController pour ajouter des modules
            FXMLLoader loaderModule = new FXMLLoader(getClass().getResource("/module.fxml"));
            Parent rootModule = loaderModule.load();

            ModuleController moduleController = loaderModule.getController();
            moduleController.setCoursId(cours.getId());
            moduleController.setModeAjoutApresCours(true);

            Stage stageModule = new Stage();
            stageModule.setTitle("Ajouter des modules - " + cours.getTitre());
            stageModule.setScene(new Scene(rootModule));
            stageModule.initModality(Modality.APPLICATION_MODAL);

            // Attendre que la fenêtre des modules soit fermée
            stageModule.showAndWait();

            // Proposer d'ajouter des leçons
            if (moduleController.aDesModulesAjoutes()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("✅ Cours ajouté");
                alert.setHeaderText("Cours '" + cours.getTitre() + "' ajouté avec succès !");
                alert.setContentText("Voulez-vous ajouter des leçons maintenant ?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    ouvrirAjoutLecons(cours.getId());
                }
            }

            refresh();
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ouvrirAjoutLecons(int coursId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Parent root = loader.load();

            LeconController leconController = loader.getController();
            leconController.setCoursId(coursId);

            Stage stage = new Stage();
            stage.setTitle("Ajouter des leçons");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // MODIFIER COURS
    // ============================================
    @FXML
    private void modifier() {
        if (coursSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours à modifier !");
            return;
        }

        try {
            if (!validateForm()) return;

            coursSelectionne.setTitre(txtTitre.getText());
            coursSelectionne.setDescription(txtDescription.getText());
            coursSelectionne.setDuree(Integer.parseInt(txtDuree.getText()));
            coursSelectionne.setNiveau(comboNiveau.getValue());
            coursSelectionne.setCompetences_visees(txtCompetences.getText());
            coursSelectionne.setEst_obligatoire(chkObligatoire.isSelected());

            if (imageBytesSelected != null) {
                coursSelectionne.setImageCouverture(imageBytesSelected);
            }

            coursService.update(coursSelectionne);
            tableCours.refresh();

            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Cours modifié avec succès !");
            clearForm();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
        }
    }

    // ============================================
    // SUPPRIMER COURS
    // ============================================
    @FXML
    private void supprimer() {
        if (coursSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours à supprimer !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation");
        confirm.setHeaderText("Supprimer le cours");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer le cours :\n\"" + coursSelectionne.getTitre() + "\" ?\n\nTous les modules et leçons associés seront également supprimés !");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                coursService.supprimer(coursSelectionne.getId());
                showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Cours supprimé !");
                refresh();
                clearForm();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void refresh() {
        try {
            List<Cours> list = coursService.readByAdmin(currentUserId);
            coursList.setAll(list);
            updateCount();
            updateStatus("Liste actualisée - " + list.size() + " cours");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    // ============================================
    // GESTION MANUELLE DES MODULES ET LEÇONS
    // ============================================
    @FXML
    private void gererModules() {
        if (coursSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours !");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/module.fxml"));
            Parent root = loader.load();

            ModuleController controller = loader.getController();
            controller.setCoursId(coursSelectionne.getId());

            Stage stage = new Stage();
            stage.setTitle("Gestion des modules - " + coursSelectionne.getTitre());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void gererLecons() {
        if (coursSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Veuillez sélectionner un cours !");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Parent root = loader.load();

            LeconController controller = loader.getController();
            controller.setCoursId(coursSelectionne.getId());

            Stage stage = new Stage();
            stage.setTitle("Gestion des leçons - " + coursSelectionne.getTitre());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // GÉNÉRER AUTOMATIQUEMENT LE TEST FINAL
    // ============================================

    @FXML
    private void genererTestFinalAutomatique() {
        if (coursSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Sélectionnez d'abord un cours");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🤖 Génération intelligente");
        confirm.setHeaderText("Générer le test final du cours ?");
        confirm.setContentText("15 questions INTELLIGENTES seront créées à partir d'une banque de questions JavaFX.\n\nLes anciennes questions seront supprimées.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection con = utils.MyDatabase.getInstance().getConnection();

                String deleteReponses = "DELETE FROM reponse WHERE question_id IN (SELECT id FROM question_test WHERE cours_id = ?) AND question_type = 'TEST'";
                PreparedStatement ps1 = con.prepareStatement(deleteReponses);
                ps1.setInt(1, coursSelectionne.getId());
                ps1.executeUpdate();

                String deleteQuestions = "DELETE FROM question_test WHERE cours_id = ?";
                PreparedStatement ps2 = con.prepareStatement(deleteQuestions);
                ps2.setInt(1, coursSelectionne.getId());
                ps2.executeUpdate();

                // Générer avec le NOUVEAU générateur intelligent
                QuizAutoGenerator generator = new QuizAutoGenerator();
                generator.genererTestFinal(coursSelectionne.getId());

                showAlert(Alert.AlertType.INFORMATION, "✅ Succès",
                        "Test final généré avec 15 questions INTELLIGENTES !\n\nLes questions sont maintenant logiques.");

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
            }
        }
    }

    // ============================================
    // VALIDATION ET UTILITAIRES
    // ============================================
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtTitre.getText().trim().isEmpty()) errors.append("• Le titre est obligatoire\n");
        if (comboNiveau.getValue() == null) errors.append("• Le niveau est obligatoire\n");
        if (txtDuree.getText().trim().isEmpty()) {
            errors.append("• La durée est obligatoire\n");
        } else {
            try {
                Integer.parseInt(txtDuree.getText());
            } catch (NumberFormatException e) {
                errors.append("• La durée doit être un nombre\n");
            }
        }
        if (coursSelectionne == null && imageBytesSelected == null) {
            errors.append("• L'image du cours est obligatoire\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Veuillez corriger :\n\n" + errors);
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
        imageViewForm.setImage(null);
        imageBytesSelected = null;
        lblImageNom.setText("Aucune image");
        coursSelectionne = null;
    }

    private void updateCount() {
        int total = coursList.size();
        int filtered = filteredList.size();
        lblCount.setText((total == filtered ? total + " cours" : filtered + "/" + total + " cours"));
    }

    private void updateStatus(String message) {
        lblStatus.setText("📌 " + message);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}