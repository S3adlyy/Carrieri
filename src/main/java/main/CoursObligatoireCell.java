package main;

import entities.Cours;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import services.CoursService;

import java.sql.SQLException;
import java.util.Optional;

public class CoursObligatoireCell extends TableCell<Cours, Boolean> {
    private final CoursService coursService;
    private CheckBox checkBox;
    private boolean confirming;

    public CoursObligatoireCell(CoursService coursService) {
        this.coursService = coursService;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        createCheckBox();
        setText(null);
        setGraphic(checkBox);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateDisplay(getItem());
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (checkBox != null) checkBox.setSelected(item != null && item);
                setText(null);
                setGraphic(checkBox);
            } else {
                updateDisplay(item);
            }
        }
    }

    private void updateDisplay(Boolean item) {
        HBox container = new HBox(8);
        container.setAlignment(javafx.geometry.Pos.CENTER);
        Circle dot = new Circle(6);
        Label label = new Label();
        if (item != null && item) {
            dot.setFill(javafx.scene.paint.Color.valueOf("#10b981"));
            label.setText("Obligatoire");
            label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            dot.setFill(javafx.scene.paint.Color.valueOf("#6b7280"));
            label.setText("Optionnel");
            label.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
        }
        container.getChildren().addAll(dot, label);
        setText(null);
        setGraphic(container);
    }

    private void createCheckBox() {
        checkBox = new CheckBox("Obligatoire");
        checkBox.setSelected(getItem() != null && getItem());
        checkBox.setOnAction(e -> confirmEdit());
    }

    private void confirmEdit() {
        if (confirming) return;
        confirming = true;
        try {
            Boolean newValue = checkBox.isSelected();
            Boolean oldValue = getItem();

            if (newValue.equals(oldValue)) {
                cancelEdit();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier le statut");
            confirm.setContentText("Voulez-vous marquer ce cours comme " + (newValue ? "obligatoire" : "optionnel") + " ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                if (cours == null) {
                    cancelEdit();
                    return;
                }
                cours.setEst_obligatoire(newValue);
                try {
                    coursService.update(cours);
                    commitEdit(newValue);
                    getTableView().refresh();
                } catch (SQLException ex) {
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + ex.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}

