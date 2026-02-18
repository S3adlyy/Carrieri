package main;

import entities.Cours;
import javafx.scene.control.*;
import services.CoursService;
import utils.AlertUtils;

import java.sql.SQLException;
import java.util.Objects;

public class CoursNiveauCell extends TableCell<Cours, String> {
    private final CoursService coursService;
    private ComboBox<String> comboBox;
    private boolean confirming;

    public CoursNiveauCell(CoursService coursService) {
        this.coursService = coursService;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        createComboBox();
        setText(null);
        setGraphic(comboBox);
        comboBox.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (comboBox != null) comboBox.setValue(item);
                setText(null);
                setGraphic(comboBox);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    private void createComboBox() {
        comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert", "Master");
        comboBox.setValue(getItem());
        comboBox.setOnAction(e -> confirmEdit());
        comboBox.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private void confirmEdit() {
        if (confirming) return;
        confirming = true;
        try {
            String newValue = comboBox.getValue();
            String oldValue = getItem();

            if (Objects.equals(newValue, oldValue)) {
                cancelEdit();
                return;
            }

            boolean confirmed = AlertUtils.showConfirmation(
                    "✏️ Confirmation",
                    "Voulez-vous modifier le niveau du cours ?\n\n" +
                            "De: \"" + oldValue + "\"\n" +
                            "Vers: \"" + newValue + "\"",
                    "Oui, modifier",
                    "Non, annuler"
            );

            if (confirmed) {
                Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                if (cours == null) {
                    cancelEdit();
                    return;
                }
                cours.setNiveau(newValue);
                try {
                    coursService.update(cours);
                    commitEdit(newValue);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "Le niveau a été modifié avec succès.");
                } catch (SQLException ex) {
                    AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + ex.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}