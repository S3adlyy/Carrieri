package main;

import entities.Lecon;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.LeconService;
import utils.AlertUtils;

import java.util.Objects;

public class LeconTitleCell extends TableCell<Lecon, String> {
    private final LeconService leconService;
    private TextField textField;
    private boolean confirming;

    public LeconTitleCell(LeconService leconService) {
        this.leconService = leconService;
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
                if (textField != null) textField.setText(item);
                setText(null);
                setGraphic(textField);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getItem());
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
            String newValue = textField.getText().trim();
            String oldValue = getItem();

            if (Objects.equals(newValue, oldValue)) {
                cancelEdit();
                return;
            }

            if (newValue.length() < 3 || newValue.length() > 200) {
                AlertUtils.showWarning("⚠️ Validation",
                        "Le titre doit contenir entre 3 et 200 caractères.\n\n" +
                                "Votre texte actuel: " + newValue.length() + " caractères.");
                cancelEdit();
                return;
            }

            boolean confirmed = AlertUtils.showConfirmation(
                    "✏️ Confirmation",
                    "Voulez-vous modifier le titre de cette leçon ?\n\n" +
                            "De: \"" + oldValue + "\"\n" +
                            "Vers: \"" + newValue + "\"",
                    "Oui, modifier",
                    "Non, annuler"
            );

            if (confirmed) {
                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    cancelEdit();
                    return;
                }
                lecon.setTitre(newValue);
                try {
                    leconService.modifier(lecon);
                    commitEdit(newValue);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "Le titre a été modifié avec succès.");
                } catch (Exception e) {
                    AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}