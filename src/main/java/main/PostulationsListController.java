package main;

import entities.Postulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.geometry.Pos;


public class PostulationsListController {

    @FXML private TableView<Postulation> tablePostulations;
    @FXML private TableColumn<Postulation, Integer> colId;
    @FXML private TableColumn<Postulation, String> colOffreTitre; // We'll set this manually
    @FXML private TableColumn<Postulation, Integer> colCandidatId;
    @FXML private TableColumn<Postulation, LocalDateTime> colDate;
    @FXML private TableColumn<Postulation, String> colStatut;
    @FXML private TableColumn<Postulation, String> colMotivation;
    @FXML private TableColumn<Postulation, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final PostulationService service = new PostulationService();
    private final ObservableList<Postulation> data = FXCollections.observableArrayList();
    private final ObservableList<Postulation> allData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Map columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCandidatId.setCellValueFactory(new PropertyValueFactory<>("candidatId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePostulation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colMotivation.setCellValueFactory(new PropertyValueFactory<>("motivationCandidature"));

        // OffreTitre: Need to fetch offre titre, but for simplicity, assume we add a method in Postulation or fetch separately
        // For now, we'll use a custom cell factory to display "Offre ID: X" - extend to join with OffreService if needed
        colOffreTitre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                } else {
                    Postulation p = getTableRow().getItem();
                    setText("Offre ID: " + p.getOffreId()); // TODO: Fetch titre from OffreService if needed
                }
            }
        });

        // Date formatter
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : fmt.format(item));
            }
        });

        // Actions column: Edit Statut + Delete
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();

            {
                // Set icons via style classes (define in CSS)
                btnEdit.getStyleClass().addAll("btn-icon", "btn-icon-edit");
                btnEdit.setTooltip(new Tooltip("Modifier Statut"));
                btnEdit.setOnAction(event -> {
                    Postulation postulation = getTableRow().getItem();
                    if (postulation != null) {
                        handleEdit(postulation);
                    }
                });

                btnDelete.getStyleClass().addAll("btn-icon", "btn-icon-delete");
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                btnDelete.setOnAction(event -> {
                    Postulation postulation = getTableRow().getItem();
                    if (postulation != null) {
                        handleDelete(postulation);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actions = new HBox(8, btnEdit, btnDelete);
                    actions.setAlignment(Pos.CENTER);
                    setGraphic(actions);
                }
            }
        });

        tablePostulations.setItems(data);
        refreshTable();
    }

    private void refreshTable() {
        try {
            allData.setAll(service.read());
            data.setAll(allData);
            lblStatus.setText(data.size() + " postulations trouvées");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String search = txtSearch.getText().toLowerCase().trim();
        if (search.isEmpty()) {
            data.setAll(allData);
        } else {
            data.setAll(allData.filtered(p ->
                    String.valueOf(p.getOffreId()).contains(search) ||
                            String.valueOf(p.getCandidatId()).contains(search) ||
                            p.getStatut().toLowerCase().contains(search)
            ));
        }
        lblStatus.setText(data.size() + " postulations trouvées");
    }

    private void handleEdit(Postulation postulation) {
        // Simple dialog to change statut
        ChoiceDialog<String> dialog = new ChoiceDialog<>(postulation.getStatut(),
                FXCollections.observableArrayList("En attente", "En cours", "Acceptée", "Refusée"));
        dialog.setTitle("Modifier Statut");
        dialog.setHeaderText("Changer le statut de la postulation ID: " + postulation.getId());
        dialog.setContentText("Nouveau statut:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(statut -> {
            try {
                service.changerStatut(postulation.getId(), statut);
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        });
    }

    private void handleDelete(Postulation postulation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la postulation ?");
        confirm.setContentText("Voulez-vous vraiment supprimer la postulation ID: " + postulation.getId() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimer(postulation.getId());
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}