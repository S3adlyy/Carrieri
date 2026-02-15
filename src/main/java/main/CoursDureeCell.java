package main;

import entities.Cours;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.CoursService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Objects;

public class CoursDureeCell extends TableCell<Cours, Integer> {
    private final CoursService coursService;
    private TextField textField;
    private boolean confirming;

    public CoursDureeCell(CoursService coursService) {
        this.coursService = coursService;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        createTextField();
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem() != null ? getItem().toString() : "");
        setGraphic(null);
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) textField.setText(item.toString());
                setText(null);
                setGraphic(textField);
            } else {
                setText(item.toString());
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getItem() != null ? getItem().toString() : "");
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                confirmEdit();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) confirmEdit();
        });
    }

    private void confirmEdit() {
        if (confirming) return;
        confirming = true;
        try {
            String newValueStr = textField.getText().trim();
            Integer oldValue = getItem();

            if (newValueStr.isEmpty()) {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "La durée ne peut pas être vide");
                cancelEdit();
                return;
            }

            try {
                int newValue = Integer.parseInt(newValueStr);
                if (newValue <= 0 || newValue > 1000) {
                    AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "La durée doit être entre 1 et 1000 heures");
                    cancelEdit();
                    return;
                }

                if (Objects.equals(newValue, oldValue)) {
                    cancelEdit();
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("✏️ Confirmation");
                confirm.setHeaderText("Modifier la durée");
                confirm.setContentText("De: " + oldValue + " heures\nVers: " + newValue + " heures");

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                    if (cours == null) {
                        cancelEdit();
                        return;
                    }
                    cours.setDuree(newValue);
                    try {
                        coursService.update(cours);
                        commitEdit(newValue);
                        getTableView().refresh();
                    } catch (SQLException e) {
                        AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "La durée doit être un nombre entier");
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}
