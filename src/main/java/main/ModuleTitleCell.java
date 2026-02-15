package main;

import entities.Module;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.ModuleService;

import java.util.Optional;
import java.util.Objects;

public class ModuleTitleCell extends TableCell<Module, String> {
    private final ModuleService moduleService;
    private TextField textField;
    private boolean confirming;

    public ModuleTitleCell(ModuleService moduleService) {
        this.moduleService = moduleService;
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
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Le titre doit contenir entre 3 et 200 caractères");
                cancelEdit();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier le titre du module");
            confirm.setContentText("De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Module module = getTableRow() != null ? getTableRow().getItem() : null;
                if (module == null) {
                    cancelEdit();
                    return;
                }
                module.setTitre(newValue);
                try {
                    moduleService.modifier(module);
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
