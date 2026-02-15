package main;

import entities.OffreEmploi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class OffresListController {

    @FXML private TableView<OffreEmploi> tableOffres;
    @FXML private TableColumn<OffreEmploi, String> colTitre;
    @FXML private TableColumn<OffreEmploi, String> colEntreprise;
    @FXML private TableColumn<OffreEmploi, String> colType;
    @FXML private TableColumn<OffreEmploi, String> colLocalisation;
    @FXML private TableColumn<OffreEmploi, Double> colSalaire;
    @FXML private TableColumn<OffreEmploi, LocalDateTime> colExpiration;
    @FXML private TableColumn<OffreEmploi, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final OffreEmploiService service = new OffreEmploiService();
    private final ObservableList<OffreEmploi> data = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Map columns
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colEntreprise.setCellValueFactory(new PropertyValueFactory<>("entreprise"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colExpiration.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));

        // Date formatter
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colExpiration.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : fmt.format(item));
            }
        });

        // Salaire formatter
        colSalaire.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : String.format("%.0f DT", item));
            }
        });

        // Actions column: Postuler + Edit + Delete (with icons)
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<OffreEmploi, Void> call(TableColumn<OffreEmploi, Void> param) {
                return new TableCell<>() {

                    private final Button btnPostuler = new Button();
                    private final Button btnEdit = new Button();
                    private final Button btnDelete = new Button();

                    {
                        // Set icons using unicode
                        btnPostuler.setGraphic(new Label("\u2714")); // Checkmark for apply
                        btnPostuler.getStyleClass().addAll("btn-icon", "btn-icon-apply");
                        btnPostuler.setTooltip(new Tooltip("Postuler"));
                        btnPostuler.setOnAction(event -> {
                            OffreEmploi offre = getTableRow().getItem();
                            if (offre != null) {
                                OffresShellController.getInstance().showPostuler(offre.getId(), offre.getTitre());
                            }
                        });

                        btnEdit.setGraphic(new Label("\u270E")); // Pencil for edit
                        btnEdit.getStyleClass().addAll("btn-icon", "btn-icon-edit");
                        btnEdit.setTooltip(new Tooltip("Modifier"));
                        btnEdit.setOnAction(event -> {
                            OffreEmploi offre = getTableRow().getItem();
                            if (offre != null) {
                                handleEdit(offre);
                            }
                        });

                        btnDelete.setGraphic(new Label("\u274C")); // X for delete
                        btnDelete.getStyleClass().addAll("btn-icon", "btn-icon-delete");
                        btnDelete.setTooltip(new Tooltip("Supprimer"));
                        btnDelete.setOnAction(event -> {
                            OffreEmploi offre = getTableRow().getItem();
                            if (offre != null) {
                                handleDelete(offre);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox actions = new HBox(8, btnPostuler, btnEdit, btnDelete);
                            actions.setAlignment(Pos.CENTER);
                            setGraphic(actions);
                        }
                    }
                };
            }
        });

        tableOffres.setItems(data);
        refreshTable();
    }

    private void refreshTable() {
        try {
            allData.setAll(service.read());
            data.setAll(allData);
            lblStatus.setText(data.size() + " offres trouvées");
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.showAndWait();
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
            data.setAll(allData.filtered(o ->
                    o.getTitre().toLowerCase().contains(search) ||
                            o.getEntreprise().toLowerCase().contains(search) ||
                            o.getLocalisation().toLowerCase().contains(search)
            ));
        }
        lblStatus.setText(data.size() + " offres trouvées");
    }

    private void handleEdit(OffreEmploi offre) {
        // ... (keep existing dialog code)
    }

    private void handleDelete(OffreEmploi offre) {
        // ... (keep existing confirm code)
    }
}