package main;

import entities.Cours;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import services.CoursService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Objects;

public class CoursDescriptionCell extends TableCell<Cours, String> {
    private final CoursService coursService;
    private TextArea textArea;
    private boolean confirming;

    public CoursDescriptionCell(CoursService coursService) {
        this.coursService = coursService;
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

            if (newValue.length() < 10 || newValue.length() > 1000) {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "La description doit contenir entre 10 et 1000 caractères");
                cancelEdit();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier la description");
            confirm.setContentText("Êtes-vous sûr de modifier la description ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                if (cours == null) {
                    cancelEdit();
                    return;
                }
                cours.setDescription(newValue);
                try {
                    coursService.update(cours);
                    commitEdit(newValue);
                    getTableView().refresh();
                } catch (SQLException e) {
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}
