package controllers;

import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import services.ReclamationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableColumns();
        loadFilterOptions();
        loadReclamations();
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

            // Colonne Catégorie (éditable avec ComboBox)
            TableColumn<Reclamation, String> categorieCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(2);
            categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
            categorieCol.setCellFactory(ComboBoxTableCell.forTableColumn("Technique", "Facturation", "Service", "Autre"));
            categorieCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setCategorie(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Statut (éditable avec ComboBox)
            TableColumn<Reclamation, String> statutCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(3);
            statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
            statutCol.setCellFactory(ComboBoxTableCell.forTableColumn("Nouvelle", "En cours", "Résolue", "Fermée"));
            statutCol.setOnEditCommit(event -> {
                Reclamation reclamation = event.getRowValue();
                reclamation.setStatut(event.getNewValue());
                updateReclamation(reclamation);
            });

            // Colonne Priorité (éditable avec ComboBox)
            TableColumn<Reclamation, String> prioriteCol = (TableColumn<Reclamation, String>) reclamationTable.getColumns().get(4);
            prioriteCol.setCellValueFactory(new PropertyValueFactory<>("priorite"));
            prioriteCol.setCellFactory(ComboBoxTableCell.forTableColumn("Haute", "Moyenne", "Basse"));
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

            // Colonne Actions (uniquement bouton Supprimer et Détails)
            TableColumn<Reclamation, Void> actionCol = (TableColumn<Reclamation, Void>) reclamationTable.getColumns().get(6);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑️");
                private final Button detailsBtn = new Button("📋");

                {
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
                    detailsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-cursor: hand;");

                    deleteBtn.setOnAction(event -> {
                        Reclamation reclamation = getTableView().getItems().get(getIndex());
                        deleteReclamation(reclamation);
                    });

                    detailsBtn.setOnAction(event -> {
                        Reclamation reclamation = getTableView().getItems().get(getIndex());
                        showReclamationDetails(reclamation);
                    });

                    deleteBtn.setTooltip(new Tooltip("Supprimer"));
                    detailsBtn.setTooltip(new Tooltip("Détails"));
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, deleteBtn, detailsBtn);
                        buttons.setStyle("-fx-alignment: center;");
                        setGraphic(buttons);
                    }
                }
            });

            // Activer l'édition sur la table
            reclamationTable.setEditable(true);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'initialisation",
                    "Impossible d'initialiser les colonnes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Nouvelle méthode pour mettre à jour une réclamation après édition inline
    private void updateReclamation(Reclamation reclamation) {
        try {
            reclamationService.update(reclamation);
            System.out.println("✅ Réclamation #" + reclamation.getId() + " mise à jour");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de mise à jour", e.getMessage());
            loadReclamations(); // Recharger en cas d'erreur
        }
    }

    private void loadFilterOptions() {
        filterStatut.getItems().addAll("Tous", "Nouvelle", "En cours", "Résolue", "Fermée");
        filterStatut.setValue("Tous");

        filterPriorite.getItems().addAll("Toutes", "Haute", "Moyenne", "Basse");
        filterPriorite.setValue("Toutes");

        filterCategorie.getItems().addAll("Toutes", "Technique", "Facturation", "Service", "Autre");
        filterCategorie.setValue("Toutes");
    }

    private void loadReclamations() {
        try {
            reclamationList.clear();
            reclamationList.addAll(reclamationService.read());
            reclamationTable.setItems(reclamationList);
            updateTotalCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger les réclamations: " + e.getMessage());
        }
    }

    @FXML
    private void showAddReclamationForm() {
        openReclamationForm(null);
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
                    boolean matchPriorite = "Toutes".equals(priorite) || priorite.equals(r.getPriorite());
                    boolean matchCategorie = "Toutes".equals(categorie) || categorie.equals(r.getCategorie());

                    if (matchStatut && matchPriorite && matchCategorie) {
                        filteredList.add(r);
                    }
                }
                reclamationList.setAll(filteredList);
                updateTotalCount();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage", e.getMessage());
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
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de recherche", e.getMessage());
            }
        });
    }

    @FXML
    private void deleteReclamation() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deleteReclamation(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucune sélection",
                    "Veuillez sélectionner une réclamation à supprimer.");
        }
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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation supprimée",
                        "La réclamation a été supprimée avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void showReclamationDetails() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showReclamationDetails(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucune sélection",
                    "Veuillez sélectionner une réclamation pour voir les détails.");
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
                "═══════════════════════════════════════";

        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openReclamationForm(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reclamationForm.fxml"));
            DialogPane dialogPane = loader.load();

            ReclamationFormController controller = loader.getController();
            controller.setReclamation(reclamation);
            controller.setReclamationList(reclamationList);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(reclamation == null ? "Ajouter une réclamation" : "Modifier la réclamation");

            dialog.showAndWait();

            loadReclamations();
            System.out.println("✅ Données rechargées");

        } catch (IOException e) {
            e.printStackTrace();
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
        showAlert(Alert.AlertType.INFORMATION, "Info", "Export PDF",
                "Fonctionnalité d'export PDF à implémenter");
    }

    @FXML
    private void exportToExcel() {
        showAlert(Alert.AlertType.INFORMATION, "Info", "Export Excel",
                "Fonctionnalité d'export Excel à implémenter");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void testAjoutRapide() {
        try {
            Reclamation test = new Reclamation();
            test.setObjet("Test rapide " + new Date());
            test.setDescription("Description test");
            test.setCategorie("Technique");
            test.setStatut("Nouvelle");
            test.setPriorite("Haute");
            test.setDateCreation(new Date());

            reclamationService.ajouter(test);
            loadReclamations();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Test réussi",
                    "Une réclamation a été ajoutée directement !");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}