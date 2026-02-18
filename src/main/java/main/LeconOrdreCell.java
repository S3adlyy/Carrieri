package main;

import entities.Lecon;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.LeconService;
import utils.AlertUtils;

import java.util.Objects;

public class LeconOrdreCell extends TableCell<Lecon, Integer> {
    private final LeconService leconService;
    private final ObservableList<Lecon> leconList;
    private TextField textField;
    private boolean confirming;

    public LeconOrdreCell(LeconService leconService, ObservableList<Lecon> leconList) {
        this.leconService = leconService;
        this.leconList = leconList;
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
                AlertUtils.showWarning("⚠️ Validation", "L'ordre ne peut pas être vide.");
                cancelEdit();
                return;
            }

            try {
                int newValue = Integer.parseInt(newValueStr);
                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "L'ordre doit être compris entre 1 et 100.\n\n" +
                                    "Valeur saisie: " + newValue);
                    cancelEdit();
                    return;
                }

                if (Objects.equals(newValue, oldValue)) {
                    cancelEdit();
                    return;
                }

                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    cancelEdit();
                    return;
                }

                final int moduleVerif = lecon.getModuleId();
                boolean ordreExiste = leconList.stream()
                        .anyMatch(l -> l.getOrdre() == newValue &&
                                l.getModuleId() == moduleVerif &&
                                l.getId() != lecon.getId());

                if (ordreExiste) {
                    AlertUtils.showWarning("⚠️ Ordre déjà utilisé",
                            "Une autre leçon a déjà l'ordre " + newValue + " dans ce module.\n\n" +
                                    "Veuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Voulez-vous modifier l'ordre de cette leçon ?\n\n" +
                                "De: " + oldValue + "\n" +
                                "Vers: " + newValue,
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    lecon.setOrdre(newValue);
                    try {
                        leconService.modifier(lecon);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "L'ordre a été modifié avec succès.");
                    } catch (Exception e) {
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                AlertUtils.showWarning("⚠️ Validation",
                        "L'ordre doit être un nombre entier.\n\n" +
                                "Valeur saisie: \"" + newValueStr + "\" n'est pas valide.");
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}