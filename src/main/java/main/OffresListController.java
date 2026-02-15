package main;

import entities.OffreEmploi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.util.Callback;
import javafx.scene.control.TableCell;

public class OffresListController {

    @FXML
    private TableView<OffreEmploi> tableOffres;

    @FXML
    private TableColumn<OffreEmploi, String> colTitre;

    @FXML
    private TableColumn<OffreEmploi, String> colDescription;

    @FXML
    private TableColumn<OffreEmploi, Double> colSalaire;

    @FXML
    private TableColumn<OffreEmploi, String> colType;

    @FXML
    private TableColumn<OffreEmploi, String> colLocalisation;

    @FXML
    private TableColumn<OffreEmploi, LocalDateTime> colExpiration;

    private final OffreEmploiService service = new OffreEmploiService();
    private final ObservableList<OffreEmploi> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // map columns to entity getters (PropertyValueFactory uses getter names)
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colExpiration.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));

        // nice date format
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colExpiration.setCellFactory(new Callback<>() {
            @Override
            public TableCell<OffreEmploi, LocalDateTime> call(TableColumn<OffreEmploi, LocalDateTime> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(LocalDateTime item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText(fmt.format(item));
                        }
                    }
                };
            }
        });

        refreshTable();
    }

    private void refreshTable() {
        try {
            data.clear();
            data.addAll(service.read()); // uses interface method
            tableOffres.setItems(data);
        } catch (SQLException e) {
            System.out.println(" Erreur chargement offres: " + e.getMessage());
        }
    }
}
