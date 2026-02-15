package main;

import entities.Lecon;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import services.LeconService;

import java.util.Optional;
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
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Le contenu doit contenir entre 10 et 5000 caractères");
                cancelEdit();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier le contenu de la leçon");
            confirm.setContentText("Êtes-vous sûr de modifier le contenu ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
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
                } catch (Exception e) {
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}
