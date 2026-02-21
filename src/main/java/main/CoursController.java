package main;

import entities.Cours;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.CoursService;
import services.ModuleService;
import services.LeconService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.QuizAutoGenerator;
import utils.AlertUtils;
import main.Main;
import main.MainShellController;
import main.CoursCell;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class CoursController implements Initializable {

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
    @FXML private VBox formPane;
    @FXML private VBox tablePane;
    @FXML private Button btnToggleForm;

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
    @FXML private TableColumn<Cours, Void> colActions;

    // BUTTONS
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnChoisirImage;
    @FXML private Label lblImageNom;
    @FXML private ImageView imageViewForm;
    @FXML private VBox card;
    @FXML private Label charCountLabel;

    // ✅ LABELS D'ERREUR
    @FXML private Label errorTitre;
    @FXML private Label errorNiveau;
    @FXML private Label errorDuree;
    @FXML private Label errorCompetences;
    @FXML private Label errorDescription;
    @FXML private Label errorImage;

    private CoursService coursService;
    private ModuleService moduleService;
    private LeconService leconService;
    private ObservableList<Cours> coursList;
    private FilteredList<Cours> filteredList;

    private int currentUserId = 1;
    private byte[] imageBytesSelected;
    private Cours coursSelectionne = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        coursService = new CoursService();
        moduleService = new ModuleService();
        leconService = new LeconService();
        coursList = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(coursList, p -> true);

        setupNiveauComboBox();
        setupTableView();
        setupSearchListener();
        setupCheckBoxListener();
        setupImageChooser();
        setupNumericFieldsOnly();

        // ✅ VALIDATION EN TEMPS RÉEL
        setupValidation();

        // ✅ LISTENER POUR LE COMPTEUR DE CARACTÈRES
        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            if (charCountLabel != null) {
                int length = newVal != null ? newVal.length() : 0;
                charCountLabel.setText(length + "/1000");

                if (length > 900) {
                    charCountLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 600; -fx-background-color: #f3e8ff; -fx-padding: 4 10; -fx-background-radius: 20;");
                } else if (length >= 1000) {
                    charCountLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: 600; -fx-background-color: #fee2e2; -fx-padding: 4 10; -fx-background-radius: 20;");
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: 600; -fx-background-color: #f3e8ff; -fx-padding: 4 10; -fx-background-radius: 20;");
                }
            }
        });

        // Start with table visible and form hidden
        setFormVisible(false);
        setTableVisible(true);

        loadCours();

        if (tablePane != null && formPane != null) {
            tablePane.setVisible(true);
            tablePane.setManaged(true);
            formPane.setVisible(false);
            formPane.setManaged(false);
            System.out.println("✅ État initial: table visible, formulaire caché");
        }

        tableCours.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                coursSelectionne = newSelection;
                // ✅ Mettre à jour le cours actif dans le shell
                MainShellController.getInstance().setCurrentCours(newSelection.getId(), newSelection.getTitre());
                chargerCoursFormulaire(newSelection);
            } else {
                coursSelectionne = null;
            }
        });

        javafx.application.Platform.runLater(() -> {
            if (tablePane != null) {
                tablePane.requestFocus();
            }
        });
    }

    // ✅ NOUVELLE MÉTHODE POUR LA VALIDATION EN TEMPS RÉEL
    private void setupValidation() {
        // Validation du titre en temps réel
        txtTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            String titre = newVal != null ? newVal.trim() : "";
            if (titre.isEmpty()) {
                showError(errorTitre, "Le titre est obligatoire");
            } else if (titre.length() < 3) {
                showError(errorTitre, "Le titre doit contenir au moins 3 caractères");
            } else if (titre.length() > 200) {
                showError(errorTitre, "Le titre ne peut pas dépasser 200 caractères");
            } else if (isOnlyDigits(titre)) {
                showError(errorTitre, "Le titre ne peut pas être composé uniquement de chiffres");
            } else {
                hideError(errorTitre);
            }
        });

        // Validation du niveau
        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(errorNiveau, "Le niveau est obligatoire");
            } else {
                hideError(errorNiveau);
            }
        });

        // Validation de la durée
        txtDuree.textProperty().addListener((obs, oldVal, newVal) -> {
            String dureeStr = newVal != null ? newVal.trim() : "";
            if (dureeStr.isEmpty()) {
                showError(errorDuree, "La durée est obligatoire");
            } else {
                try {
                    int duree = Integer.parseInt(dureeStr);
                    if (duree <= 0) {
                        showError(errorDuree, "La durée doit être un nombre positif");
                    } else if (duree > 1000) {
                        showError(errorDuree, "La durée ne peut pas dépasser 1000 heures");
                    } else {
                        hideError(errorDuree);
                    }
                } catch (NumberFormatException e) {
                    showError(errorDuree, "La durée doit être un nombre entier");
                }
            }
        });

        // Validation des compétences
        txtCompetences.textProperty().addListener((obs, oldVal, newVal) -> {
            String competences = newVal != null ? newVal.trim() : "";
            if (!competences.isEmpty() && competences.length() > 500) {
                showError(errorCompetences, "Les compétences ne peuvent pas dépasser 500 caractères");
            } else if (!competences.isEmpty() && isOnlyDigits(competences)) {
                showError(errorCompetences, "Les compétences ne peuvent pas être composées uniquement de chiffres");
            } else {
                hideError(errorCompetences);
            }
        });

        // Validation de la description
        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            String desc = newVal != null ? newVal.trim() : "";
            if (desc.isEmpty()) {
                showError(errorDescription, "La description est obligatoire");
            } else if (desc.length() < 10) {
                showError(errorDescription, "La description doit contenir au moins 10 caractères");
            } else if (desc.length() > 1000) {
                showError(errorDescription, "La description ne peut pas dépasser 1000 caractères");
            } else if (isOnlyDigits(desc)) {
                showError(errorDescription, "La description ne peut pas être composée uniquement de chiffres");
            } else {
                hideError(errorDescription);
            }
        });

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
                    hideError(errorImage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ✅ MÉTHODES UTILITAIRES POUR LES ERREURS
    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-font-weight: 600;");
        // Ajouter la classe 'error' au champ correspondant
        if (errorLabel == errorTitre) {
            txtTitre.getStyleClass().add("error");
        } else if (errorLabel == errorDescription) {
            txtDescription.getStyleClass().add("error");
        } else if (errorLabel == errorDuree) {
            txtDuree.getStyleClass().add("error");
        } else if (errorLabel == errorNiveau) {
            comboNiveau.getStyleClass().add("error");
        } else if (errorLabel == errorCompetences) {
            txtCompetences.getStyleClass().add("error");
        }
    }

    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Retirer la classe 'error' du champ
        if (errorLabel == errorTitre) {
            txtTitre.getStyleClass().remove("error");
        } else if (errorLabel == errorDescription) {
            txtDescription.getStyleClass().remove("error");
        } else if (errorLabel == errorDuree) {
            txtDuree.getStyleClass().remove("error");
        } else if (errorLabel == errorNiveau) {
            comboNiveau.getStyleClass().remove("error");
        } else if (errorLabel == errorCompetences) {
            txtCompetences.getStyleClass().remove("error");
        }
    }

    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) return false;
        String textWithoutSpaces = text.replaceAll("\\s+", "");
        return textWithoutSpaces.matches("\\d+");
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
                    hideError(errorImage);
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
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> CoursCell.getTitleCell());

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(column -> CoursCell.getDescriptionCell());

        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colDuree.setCellFactory(column -> CoursCell.getDureeCell());

        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colNiveau.setCellFactory(column -> CoursCell.getNiveauCell());

        colCompetences.setCellValueFactory(new PropertyValueFactory<>("competences_visees"));
        colCompetences.setCellFactory(column -> CoursCell.getCompetencesCell());

        colObligatoire.setCellValueFactory(new PropertyValueFactory<>("est_obligatoire"));
        colObligatoire.setCellFactory(column -> CoursCell.getObligatoireCell());

        colImage.setCellValueFactory(new PropertyValueFactory<>("imageCouverture"));
        colImage.setCellFactory(column -> CoursCell.getImageCell());

        // Colonne Actions avec bouton de suppression
        colActions.setCellFactory(param -> new TableCell<>() {

            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(6, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);
                btnDelete.getStyleClass().addAll("action-button", "btn-delete-gradient-light");

                btnDelete.setOnAction(event -> {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours != null) {
                        supprimerCoursAvecConfirmation(cours);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tableCours.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                coursSelectionne = newSelection;
            } else {
                coursSelectionne = null;
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
            hideError(errorImage);
        }
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable(newValue));
    }

    private void setupCheckBoxListener() {
        chkObligatoire.selectedProperty().addListener((observable, oldValue, newValue) -> {
            lblObligatoire.setText(newValue ? "Obligatoire" : "Optionnel");
            lblObligatoire.setStyle(newValue ?
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12px;" :
                    "-fx-background-color: #f3e8ff; -fx-text-fill: #5E548E; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12px;");
        });
    }

    private void setupNumericFieldsOnly() {
        txtDuree.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtDuree.setText(newValue.replaceAll("[^\\d]", ""));
            }
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
    // MÉTHODE PRINCIPALE : AJOUTER COURS - MODIFIÉE
    // ============================================
    @FXML
    private void ajouter() {
        try {
            if (!validateForm()) return;

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

            coursService.ajouter(nouveauCours);

            // ✅ NOUVELLE ALERTE AVEC INSTRUCTIONS
            AlertUtils.showSuccessWithInstructions(
                    "🎉 Cours créé avec succès",
                    "Le cours \"" + txtTitre.getText() + "\" a été ajouté à la plateforme.",
                    "Utilisez les icônes 📚 et 📖 pour ajouter des modules et des leçons à ce cours."
            );

            loadCours();
            clearForm();

            tablePane.setVisible(true);
            tablePane.setManaged(true);
            formPane.setVisible(false);
            formPane.setManaged(false);

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("titre")) {
                showError(errorTitre, message);
            } else if (message.contains("description")) {
                showError(errorDescription, message);
            } else if (message.contains("niveau")) {
                showError(errorNiveau, message);
            } else if (message.contains("durée") || message.contains("duree")) {
                showError(errorDuree, message);
            } else if (message.contains("compétences") || message.contains("competences")) {
                showError(errorCompetences, message);
            } else if (message.contains("image")) {
                showError(errorImage, message);
            } else {
                AlertUtils.showWarning("⚠️ Validation", message);
            }
        } catch (SQLException e) {
            AlertUtils.showError("❌ Erreur base de données", "Impossible d'ajouter le cours :\n" + e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("❌ Erreur", "Une erreur inattendue est survenue :\n" + e.getMessage());
        }
    }

    // ============================================
    // MODIFIER COURS - MODIFIÉE
    // ============================================
    @FXML
    private void modifier() {
        if (coursSelectionne == null) {
            AlertUtils.showNoSelectionWarning("cours", "modifier");
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

            AlertUtils.showSuccess("✅ Modification réussie", "Le cours a été mis à jour avec succès.");
            clearForm();
            loadCours();

        } catch (SQLException e) {
            AlertUtils.showError("❌ Erreur", e.getMessage());
        }
    }

    // ============================================
    // SUPPRIMER COURS - MODIFIÉE
    // ============================================
    @FXML
    private void supprimer() {
        if (coursSelectionne == null) {
            AlertUtils.showNoSelectionWarning("cours", "supprimer");
            return;
        }
        supprimerCoursAvecConfirmation(coursSelectionne);
    }

    private void supprimerCoursAvecConfirmation(Cours cours) {
        // ✅ NOUVELLE CONFIRMATION AVEC CONSÉQUENCES
        boolean confirmed = AlertUtils.showDeleteConfirmation(
                "cours",
                cours.getTitre(),
                "Tous les modules et leçons associés à ce cours seront également supprimés définitivement.\n\nCette action est irréversible !"
        );

        if (confirmed) {
            try {
                coursService.supprimer(cours.getId());

                if (coursSelectionne != null && coursSelectionne.getId() == cours.getId()) {
                    MainShellController.getInstance().resetCurrentCours();
                }

                AlertUtils.showSuccess("✅ Suppression réussie", "Le cours a été supprimé avec succès.");
                loadCours();
                clearForm();
            } catch (SQLException e) {
                AlertUtils.showError("❌ Erreur", "Impossible de supprimer le cours :\n" + e.getMessage());
            }
        }
    }

    private void loadCours() {
        try {
            List<Cours> list = coursService.readByAdmin(currentUserId);
            coursList.setAll(list);
            updateCount();
            updateStatus("Liste actualisée - " + list.size() + " cours");
        } catch (SQLException e) {
            AlertUtils.showError("❌ Erreur", "Erreur lors du chargement des cours :\n" + e.getMessage());
        }
    }

    // ============================================
    // GESTION MANUELLE DES MODULES ET LEÇONS
    // ============================================
    @FXML
    private void gererModules() {
        if (coursSelectionne == null) {
            AlertUtils.showNoSelectionWarning("cours", "gérer ses modules");
            return;
        }
        ouvrirGestionModules(coursSelectionne);
    }

    @FXML
    private void gererLecons() {
        if (coursSelectionne == null) {
            AlertUtils.showNoSelectionWarning("cours", "gérer ses leçons");
            return;
        }
        ouvrirGestionLecons(coursSelectionne);
    }

    private void ouvrirGestionModules(Cours cours) {
        try {
            System.out.println("📚 Ouverture gestion modules pour cours ID: " + cours.getId() + " - " + cours.getTitre());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/module.fxml"));
            Parent root = loader.load();

            ModuleController controller = loader.getController();
            controller.setCoursId(cours.getId());

            Stage stage = new Stage();
            stage.setTitle("Gestion des modules - " + cours.getTitre());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            System.out.println("✅ Fenêtre modules ouverte avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir la gestion des modules:\n" + e.getMessage());
        }
    }

    private void ouvrirGestionLecons(Cours cours) {
        try {
            System.out.println("📖 Ouverture gestion leçons pour cours ID: " + cours.getId() + " - " + cours.getTitre());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Parent root = loader.load();

            LeconController controller = loader.getController();
            controller.setCoursId(cours.getId());

            Stage stage = new Stage();
            stage.setTitle("Gestion des leçons - " + cours.getTitre());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            System.out.println("✅ Fenêtre leçons ouverte avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir la gestion des leçons:\n" + e.getMessage());
        }
    }

    // ============================================
    // GÉNÉRER TEST FINAL - MODIFIÉE
    // ============================================
    @FXML
    public void genererTestFinalAutomatique() {
        if (coursSelectionne == null) {
            AlertUtils.showNoSelectionWarning("cours", "générer son test final");
            return;
        }

        List<entities.Module> modules = moduleService.getModulesByCours(coursSelectionne.getId());
        if (modules == null || modules.isEmpty()) {
            AlertUtils.showWarning("⚠️ Impossible de générer le test",
                    "Le cours \"" + coursSelectionne.getTitre() + "\" ne contient aucun module.\n\n" +
                            "Ajoutez d'abord des modules à ce cours pour pouvoir générer un test final.");
            return;
        }

        boolean hasLecons = modules.stream().anyMatch(m -> {
            List<entities.Lecon> lecons = leconService.getLeconsByModule(m.getId());
            return lecons != null && !lecons.isEmpty();
        });
        if (!hasLecons) {
            AlertUtils.showWarning("⚠️ Impossible de générer le test",
                    "Les modules de ce cours sont vides (aucune leçon).\n\n" +
                            "Ajoutez d'abord des leçons aux modules pour générer un test final.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation(
                "🤖 Génération intelligente du test final",
                "15 questions INTELLIGENTES seront créées à partir d'une banque de questions JavaFX.\n\n" +
                        "⚠️ Les anciennes questions du test final seront définitivement supprimées.\n\n" +
                        "Voulez-vous continuer ?",
                "Oui, générer",
                "Non, annuler"
        );

        if (confirmed) {
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

                QuizAutoGenerator generator = new QuizAutoGenerator();
                generator.genererTestFinal(coursSelectionne.getId());

                AlertUtils.showSuccess("✅ Test final généré avec succès",
                        "Le test final du cours \"" + coursSelectionne.getTitre() + "\" a été généré avec 15 questions intelligentes.\n\n" +
                                "Les candidats pourront maintenant passer ce test après avoir terminé tous les modules.");

            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtils.showError("❌ Erreur", "Impossible de générer le test final :\n" + e.getMessage());
            }
        }
    }

    // ============================================
    // VALIDATION ET UTILITAIRES
    // ============================================
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
            showError(errorTitre, "Le titre est obligatoire");
        } else if (titre.length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
            showError(errorTitre, "Le titre doit contenir au moins 3 caractères");
        } else if (titre.length() > 200) {
            errors.append("• Le titre ne peut pas dépasser 200 caractères\n");
            showError(errorTitre, "Le titre ne peut pas dépasser 200 caractères");
        } else if (isOnlyDigits(titre)) {
            errors.append("• Le titre ne peut pas être composé uniquement de chiffres\n");
            showError(errorTitre, "Le titre ne peut pas être composé uniquement de chiffres");
        } else {
            hideError(errorTitre);
        }

        String description = txtDescription.getText().trim();
        if (description.isEmpty()) {
            errors.append("• La description est obligatoire\n");
            showError(errorDescription, "La description est obligatoire");
        } else if (description.length() < 10) {
            errors.append("• La description doit contenir au moins 10 caractères\n");
            showError(errorDescription, "La description doit contenir au moins 10 caractères");
        } else if (description.length() > 1000) {
            errors.append("• La description ne peut pas dépasser 1000 caractères\n");
            showError(errorDescription, "La description ne peut pas dépasser 1000 caractères");
        } else if (isOnlyDigits(description)) {
            errors.append("• La description ne peut pas être composée uniquement de chiffres\n");
            showError(errorDescription, "La description ne peut pas être composée uniquement de chiffres");
        } else {
            hideError(errorDescription);
        }

        if (comboNiveau.getValue() == null) {
            errors.append("• Le niveau est obligatoire\n");
            showError(errorNiveau, "Le niveau est obligatoire");
        } else {
            hideError(errorNiveau);
        }

        if (txtDuree.getText().trim().isEmpty()) {
            errors.append("• La durée est obligatoire\n");
            showError(errorDuree, "La durée est obligatoire");
        } else {
            try {
                int duree = Integer.parseInt(txtDuree.getText().trim());
                if (duree <= 0) {
                    errors.append("• La durée doit être un nombre positif\n");
                    showError(errorDuree, "La durée doit être un nombre positif");
                } else if (duree > 1000) {
                    errors.append("• La durée ne peut pas dépasser 1000 heures\n");
                    showError(errorDuree, "La durée ne peut pas dépasser 1000 heures");
                } else {
                    hideError(errorDuree);
                }
            } catch (NumberFormatException e) {
                errors.append("• La durée doit être un nombre entier valide\n");
                showError(errorDuree, "La durée doit être un nombre entier valide");
            }
        }

        String competences = txtCompetences.getText().trim();
        if (!competences.isEmpty() && competences.length() > 500) {
            errors.append("• Les compétences ne peuvent pas dépasser 500 caractères\n");
            showError(errorCompetences, "Les compétences ne peuvent pas dépasser 500 caractères");
        } else if (!competences.isEmpty() && isOnlyDigits(competences)) {
            errors.append("• Les compétences ne peuvent pas être composées uniquement de chiffres\n");
            showError(errorCompetences, "Les compétences ne peuvent pas être composées uniquement de chiffres");
        } else {
            hideError(errorCompetences);
        }

        if (coursSelectionne == null && imageBytesSelected == null) {
            errors.append("• L'image du cours est obligatoire\n");
            showError(errorImage, "L'image du cours est obligatoire");
        } else {
            hideError(errorImage);
        }

        return errors.length() == 0;
    }

    @FXML
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
        tableCours.getSelectionModel().clearSelection();

        // ✅ RETIRER LES CLASSES ERROR
        txtTitre.getStyleClass().remove("error");
        txtDescription.getStyleClass().remove("error");
        txtDuree.getStyleClass().remove("error");
        txtCompetences.getStyleClass().remove("error");
        comboNiveau.getStyleClass().remove("error");

        hideError(errorTitre);
        hideError(errorNiveau);
        hideError(errorDuree);
        hideError(errorCompetences);
        hideError(errorDescription);
        hideError(errorImage);

        if (charCountLabel != null) {
            charCountLabel.setText("0/1000");
            charCountLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: 600; -fx-background-color: #f3e8ff; -fx-padding: 4 10; -fx-background-radius: 20;");
        }
    }

    private void updateCount() {
        int total = coursList.size();
        int filtered = filteredList.size();
        lblCount.setText((total == filtered ? total + " cours" : filtered + "/" + total + " cours"));
    }

    private void updateStatus(String message) {
        lblStatus.setText("📌 " + message);
    }

    private void setFormVisible(boolean visible) {
        if (formPane != null) {
            formPane.setVisible(visible);
            formPane.setManaged(visible);
        }
        if (btnToggleForm != null) {
            btnToggleForm.setText(visible ? "✖️" : "➕");
            btnToggleForm.setStyle(visible ?
                    "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-size: 16px; -fx-padding: 8 12;" :
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-size: 16px; -fx-padding: 8 12;");
        }
    }

    private void setTableVisible(boolean visible) {
        if (tablePane != null) {
            tablePane.setVisible(visible);
            tablePane.setManaged(visible);
        }
    }

    @FXML
    private void toggleForm() {
        System.out.println("🔄 toggleForm appelé - table visible: " + (tablePane != null ? tablePane.isVisible() : "null"));

        if (tablePane != null && formPane != null) {
            boolean isTableVisible = tablePane.isVisible();

            if (isTableVisible) {
                tablePane.setVisible(false);
                tablePane.setManaged(false);
                formPane.setVisible(true);
                formPane.setManaged(true);
                clearForm();
                System.out.println("📝 Formulaire affiché");
            } else {
                tablePane.setVisible(true);
                tablePane.setManaged(true);
                formPane.setVisible(false);
                formPane.setManaged(false);
                System.out.println("📋 Table affichée");
            }
        } else {
            System.err.println("❌ tablePane ou formPane est null!");
        }
    }
}