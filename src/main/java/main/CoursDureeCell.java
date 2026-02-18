package main;

import entities.Cours;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.CoursService;
import utils.AlertUtils;

import java.sql.SQLException;
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
                AlertUtils.showWarning("⚠️ Validation", "La durée ne peut pas être vide.");
                cancelEdit();
                return;
            }

            try {
                int newValue = Integer.parseInt(newValueStr);
                if (newValue <= 0 || newValue > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La durée doit être comprise entre 1 et 1000 heures.\n\n" +
                                    "Valeur saisie: " + newValue + " heures.");
                    cancelEdit();
                    return;
                }

                if (Objects.equals(newValue, oldValue)) {
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Voulez-vous modifier la durée du cours ?\n\n" +
                                "De: " + oldValue + " heures\n" +
                                "Vers: " + newValue + " heures",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
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
                        AlertUtils.showSuccess("✅ Succès", "La durée a été modifiée avec succès.");
                    } catch (SQLException e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                AlertUtils.showWarning("⚠️ Validation",
                        "La durée doit être un nombre entier.\n\n" +
                                "Valeur saisie: \"" + newValueStr + "\" n'est pas un nombre valide.");
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}