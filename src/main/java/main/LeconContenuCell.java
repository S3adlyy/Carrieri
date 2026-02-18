package main;

import entities.Lecon;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import services.LeconService;
import utils.AlertUtils;

import java.util.Objects;

public class LeconContenuCell extends TableCell<Lecon, String> {
    private final LeconService leconService;
    private TextArea textArea;
    private boolean confirming;

    public LeconContenuCell(LeconService leconService) {
        this.leconService = leconService;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        createTextArea();
        setText(null);
        setGraphic(textArea);
        textArea.selectAll();
        textArea.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem() != null ? getItem().substring(0, Math.min(20, getItem().length())) + "..." : "");
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
                if (textArea != null) textArea.setText(item);
                setText(null);
                setGraphic(textArea);
            } else {
                setText(item != null ? item.substring(0, Math.min(20, item.length())) + "..." : "");
                setGraphic(null);
            }
        }
    }

    private void createTextArea() {
        textArea = new TextArea(getItem());
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        textArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) confirmEdit();
        });
    }

    private void confirmEdit() {
        if (confirming) return;
        confirming = true;
        try {
            String newValue = textArea.getText().trim();
            String oldValue = getItem();

            if (Objects.equals(newValue, oldValue)) {
                cancelEdit();
                return;
            }

            if (newValue.length() < 10 || newValue.length() > 5000) {
                AlertUtils.showWarning("⚠️ Validation",
                        "Le contenu doit contenir entre 10 et 5000 caractères.\n\n" +
                                "Votre texte actuel: " + newValue.length() + " caractères.");
                cancelEdit();
                return;
            }

            boolean confirmed = AlertUtils.showConfirmation(
                    "✏️ Confirmation",
                    "Êtes-vous sûr de vouloir modifier le contenu de cette leçon ?",
                    "Oui, modifier",
                    "Non, annuler"
            );

            if (confirmed) {
                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    cancelEdit();
                    return;
                }
                lecon.setContenu(newValue);
                try {
                    leconService.modifier(lecon);
                    commitEdit(newValue);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "Le contenu a été modifié avec succès.");
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