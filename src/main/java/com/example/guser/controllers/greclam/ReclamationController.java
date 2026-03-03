package com.example.guser.controllers.greclam;

import entities.greclam.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import services.greclam.PrioriteService;
import services.greclam.ReclamationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReclamationController implements Initializable {

    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterPriorite;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private Label totalLabel;

    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList();
    private ReclamationService reclamationService = new ReclamationService();
    private PrioriteService prioriteService = new PrioriteService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Debug : afficher l'URL de chargement
        System.out.println("Initialisation du contrôleur...");
        System.out.println("URL de localisation: " + location);
        System.out.println("Répertoire de travail: " + System.getProperty("user.dir"));

        // Vérifier si la table est correctement injectée
        if (reclamationTable == null) {
            System.err.println("❌ ERREUR: reclamationTable est null! Vérifiez fx:id dans le FXML");
        } else {
            System.out.println("✅ reclamationTable injectée avec succès");
        }

        initializeTableColumns();
        loadFilterOptions();
        loadReclamations();
        reclamationTable.setEditable(true);
    }

    @SuppressWarnings("unchecked")
    private void initializeTableColumns() {
        try {
            // Colonne ID (non éditable)
            TableColumn<Reclamation, Integer> idCol = (TableColumn<Reclamation, Integer>) reclamationTable.getColumns().get(0);
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

            // Colonne Objet (éditable)
            TableColumn<Reclamation, String> objetCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(1);
            objetCol.setCellValueFactory(new PropertyValueFactory<>("objet"));
            objetCol.setCellFactory(TextFieldTableCell.forTableColumn());
            objetCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setObjet(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Catégorie (éditable)
            TableColumn<Reclamation, String> categorieCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(2);
            categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
            categorieCol.setCellFactory(ComboBoxTableCell.forTableColumn("Technique", "Facturation", "Service", "Autre"));
            categorieCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setCategorie(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Statut (éditable)
            TableColumn<Reclamation, String> statutCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(3);
            statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
            statutCol.setCellFactory(ComboBoxTableCell.forTableColumn("Nouvelle", "En cours", "Résolue", "Fermée"));
            statutCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setStatut(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Priorité avec couleurs (éditable)
            TableColumn<Reclamation, String> prioriteCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(4);
            prioriteCol.setCellValueFactory(new PropertyValueFactory<>("priorite"));

            // Personnaliser l'affichage avec couleurs (sans ComboBox pour éviter les conflits)
            prioriteCol.setCellFactory(column -> new TableCell<Reclamation, String>() {
                @Override
                protected void updateItem(String priorite, boolean empty) {
                    super.updateItem(priorite, empty);

                    if (empty || priorite == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(priorite);

                        // Appliquer le style selon la priorité
                        if (priorite.contains("URGENT")) {
                            setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        } else if (priorite.contains("HAUTE")) {
                            setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        } else if (priorite.contains("MOYENNE")) {
                            setStyle("-fx-background-color: #f1c40f; -fx-font-weight: bold; -fx-background-radius: 5;");
                        } else if (priorite.contains("BASSE")) {
                            setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        }
                    }
                }
            });

            prioriteCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setPriorite(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Date (non éditable)
            TableColumn<Reclamation, Date> dateCol = (TableColumn<Reclamation, Date>) reclamationTable.getColumns().get(5);
            dateCol.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
            dateCol.setCellFactory(column -> new TableCell<Reclamation, Date>() {
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

            // Colonne Actions (boutons Traiter, Détails, Supprimer)
            TableColumn<Reclamation, Void> actionCol = (TableColumn<Reclamation, Void>) reclamationTable.getColumns().get(6);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button traiterBtn = new Button("⚡");
                private final Button detailsBtn = new Button("📋");
                private final Button deleteBtn = new Button("🗑️");

                {
                    traiterBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-cursor: hand;");
                    detailsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");

                    traiterBtn.setTooltip(new Tooltip("Traiter cette réclamation"));
                    detailsBtn.setTooltip(new Tooltip("Détails"));
                    deleteBtn.setTooltip(new Tooltip("Supprimer"));

                    traiterBtn.setOnAction(event -> {
                        Reclamation reclamation = getTableView().getItems().get(getIndex());
                        ouvrirFormulaireTraitement(reclamation);
                    });

                    detailsBtn.setOnAction(event -> {
                        Reclamation reclamation = getTableView().getItems().get(getIndex());
                        showReclamationDetails(reclamation);
                    });

                    deleteBtn.setOnAction(event -> {
                        Reclamation reclamation = getTableView().getItems().get(getIndex());
                        deleteReclamation(reclamation);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, traiterBtn, detailsBtn, deleteBtn);
                        buttons.setStyle("-fx-alignment: center;");
                        setGraphic(buttons);
                    }
                }
            });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateReclamation(Reclamation reclamation) {
        try {
            // Recalculer la priorité avant sauvegarde
            String nouvellePriorite = prioriteService.calculerPriorite(reclamation);
            reclamation.setPriorite(nouvellePriorite);

            reclamationService.update(reclamation);
            System.out.println("✅ Réclamation #" + reclamation.getId() + " mise à jour avec priorité: " + nouvellePriorite);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de mise à jour: " + e.getMessage());
            loadReclamations();
        }
    }

    private void loadFilterOptions() {
        filterStatut.getItems().addAll("Tous", "Nouvelle", "En cours", "Résolue", "Fermée");
        filterStatut.setValue("Tous");

        filterPriorite.getItems().addAll("Toutes", "🔴 URGENT", "🟠 HAUTE", "🟡 MOYENNE", "🟢 BASSE");
        filterPriorite.setValue("Toutes");

        filterCategorie.getItems().addAll("Toutes", "Technique", "Facturation", "Service", "Autre");
        filterCategorie.setValue("Toutes");
    }


    private void loadReclamations() {
        try {
            reclamationList.clear();
            List<Reclamation> list = reclamationService.read();

            // Mettre à jour les priorités automatiquement
            for (Reclamation r : list) {
                String nouvellePriorite = prioriteService.calculerPriorite(r);
                System.out.print("priorite in load reclam: " + nouvellePriorite);
                r.setPriorite(nouvellePriorite);
            }

            reclamationList.addAll(list);
            reclamationTable.setItems(reclamationList);
            updateTotalCount();
            System.out.println("✅ " + reclamationList.size() + " réclamations chargées avec priorités");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réclamations: " + e.getMessage());
        }
    }

    @FXML
    private void filterReclamations() {
        try {
            String statut = filterStatut.getValue();
            String priorite = filterPriorite.getValue();
            String categorie = filterCategorie.getValue();

            if ("Tous".equals(statut) && "Toutes".equals(priorite) && "Toutes".equals(categorie)) {
                loadReclamations();
            } else {
                ObservableList<Reclamation> filteredList = FXCollections.observableArrayList();
                for (Reclamation r : reclamationService.read()) {
                    boolean matchStatut = "Tous".equals(statut) || statut.equals(r.getStatut());
                    boolean matchPriorite = "Toutes".equals(priorite) ||
                            (r.getPriorite() != null && r.getPriorite().contains(priorite.replace("🔴 ", "").replace("🟠 ", "").replace("🟡 ", "").replace("🟢 ", "")));
                    boolean matchCategorie = "Toutes".equals(categorie) || categorie.equals(r.getCategorie());

                    if (matchStatut && matchPriorite && matchCategorie) {
                        filteredList.add(r);
                    }
                }
                reclamationList.setAll(filteredList);
                updateTotalCount();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage: " + e.getMessage());
        }
    }

    @FXML
    private void showSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rechercher");
        dialog.setHeaderText("Rechercher une réclamation");
        dialog.setContentText("Mot-clé:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(keyword -> {
            try {
                if (keyword.trim().isEmpty()) {
                    loadReclamations();
                } else {
                    reclamationList.setAll(reclamationService.searchByKeyword(keyword));
                    updateTotalCount();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de recherche: " + e.getMessage());
            }
        });
    }

    private void deleteReclamation(Reclamation reclamation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la réclamation");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette réclamation ?\n\n" +
                "Objet: " + reclamation.getObjet());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reclamationService.supprimer(reclamation.getId());
                reclamationList.remove(reclamation);
                updateTotalCount();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation supprimée avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression: " + e.getMessage());
            }
        }
    }

    private void showReclamationDetails(Reclamation reclamation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la réclamation");
        alert.setHeaderText("Détails de la réclamation #" + reclamation.getId());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateStr = reclamation.getDateCreation() != null ?
                dateFormat.format(reclamation.getDateCreation()) : "Non définie";

        String content = "═══════════════════════════════════════\n" +
                "📋 OBJET:\n" + reclamation.getObjet() + "\n\n" +
                "📝 DESCRIPTION:\n" + reclamation.getDescription() + "\n\n" +
                "═══════════════════════════════════════\n" +
                "🏷️ Catégorie: " + reclamation.getCategorie() + "\n" +
                "📊 Statut: " + reclamation.getStatut() + "\n" +
                "⚠️ Priorité: " + reclamation.getPriorite() + "\n" +
                "📅 Date: " + dateStr + "\n" +
                "👤 Utilisateur ID: " + (reclamation.getUtilisateurId() != null ? reclamation.getUtilisateurId() : "N/A") + "\n" +
                "📧 Email: " + (reclamation.getEmail() != null ? reclamation.getEmail() : "Non renseigné") + "\n" +
                "═══════════════════════════════════════";

        alert.setContentText(content);
        alert.showAndWait();
    }

    // ✅ Méthode pour ouvrir le formulaire de traitement
    private void ouvrirFormulaireTraitement(Reclamation reclamation) {
        try {
            // Debug: Print current class location
            System.out.println("Current class: " + getClass().getName());
            System.out.println("Current package: " + getClass().getPackage().getName());

            // Try different possible paths
            String[] pathsToTry = {
                    "/com/example/guser/greclam/traitementForm.fxml",
                    "/fxml/traitementForm.fxml",
                    "/traitementForm.fxml",
                    "traitementForm.fxml",
                    "../traitementForm.fxml",
                    "../../traitementForm.fxml"
            };

            URL fxmlUrl = null;
            String successfulPath = null;

            for (String path : pathsToTry) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) {
                    successfulPath = path;
                    System.out.println("✅ Found traitementForm.fxml at: " + path);
                    break;
                } else {
                    System.out.println("❌ Not found: " + path);
                }
            }

            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Fichier traitementForm.fxml introuvable. Vérifiez le chemin.");
                return;
            }

            // Get the MainController instance
            MainController mainController = MainController.getInstance();

            // Load the traitement form
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            TraitementFormController controller = loader.getController();
            controller.setReclamation(reclamation);
            controller.setMode("TRAITEMENT");

            // Set the content
            if (mainController != null) {
                mainController.setContent(root);
            } else {
                // Try to get from scene user data
                Object userData = reclamationTable.getScene().getUserData();
                if (userData instanceof MainController) {
                    mainController = (MainController) userData;
                    mainController.setContent(root);
                } else {
                    // Last resort: new window
                    Stage stage = new Stage();
                    stage.setTitle("Traiter la réclamation");
                    stage.setScene(new Scene(root));
                    stage.show();
                }
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de traitement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Méthode pour ouvrir le formulaire de réclamation (ajout/modification)
    private void openReclamationForm(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/greclam/reclamationForm.fxml"));
            Parent root = loader.load();

            ReclamationFormController controller = loader.getController();
            controller.setReclamation(reclamation);

            // Créer une nouvelle fenêtre (Stage)
            Stage stage = new Stage();
            stage.setTitle(reclamation == null ? "Ajouter une réclamation" : "Modifier la réclamation");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            // Attendre la fermeture
            stage.showAndWait();

            // Recharger la liste après fermeture
            loadReclamations();
            System.out.println("✅ Données rechargées après fermeture du formulaire");

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showAddReclamationForm() {
        openReclamationForm(null);
    }

    @FXML
    private void mettreAJourPriorites() {
        try {
            List<Reclamation> list = reclamationService.read();
            int count = 0;

            for (Reclamation r : list) {
                String anciennePriorite = r.getPriorite();
                String nouvellePriorite = prioriteService.calculerPriorite(r);

                if (!anciennePriorite.equals(nouvellePriorite)) {
                    r.setPriorite(nouvellePriorite);
                    System.out.print("priorite: " + nouvellePriorite);

                    reclamationService.update(r);
                    count++;
                }
            }

            loadReclamations();
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    count + " réclamation(s) ont eu leur priorité mise à jour.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    private void updateTotalCount() {
        totalLabel.setText("Total: " + reclamationList.size());

        if (reclamationList.size() > 20) {
            totalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        } else if (reclamationList.size() > 10) {
            totalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12;");
        } else {
            totalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
        }
    }

    @FXML
    private void exportToPDF() {
        showAlert(Alert.AlertType.INFORMATION, "Info", "Export PDF à implémenter");
    }

    @FXML
    private void exportToExcel() {
        showAlert(Alert.AlertType.INFORMATION, "Info", "Export Excel à implémenter");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}