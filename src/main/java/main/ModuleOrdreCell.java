package main;

import entities.Module;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import services.ModuleService;

import java.util.Optional;
import java.util.Objects;

public class ModuleOrdreCell extends TableCell<Module, Integer> {
    private final ModuleService moduleService;
    private final ObservableList<Module> moduleList;
    private TextField textField;
    private boolean confirming;

    public ModuleOrdreCell(ModuleService moduleService, ObservableList<Module> moduleList) {
        this.moduleService = moduleService;
        this.moduleList = moduleList;
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
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "L'ordre ne peut pas être vide");
                cancelEdit();
                return;
            }

            try {
                int newValue = Integer.parseInt(newValueStr);
                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "L'ordre doit être entre 1 et 100");
                    cancelEdit();
                    return;
                }

                if (Objects.equals(newValue, oldValue)) {
                    cancelEdit();
                    return;
                }

                Module module = getTableRow() != null ? getTableRow().getItem() : null;
                if (module == null) {
                    cancelEdit();
                    return;
                }

                boolean ordreExiste = moduleList.stream()
                        .anyMatch(m -> m.getOrdre() == newValue && m.getId() != module.getId());

                if (ordreExiste) {
                    AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Ordre déjà utilisé",
                            "Un autre module a déjà l'ordre " + newValue + ".\nVeuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("✏️ Confirmation");
                confirm.setHeaderText("Modifier l'ordre");
                confirm.setContentText("De: " + oldValue + "\nVers: " + newValue);

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    module.setOrdre(newValue);
                    try {
                        moduleService.modifier(module);
                        commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {
                        AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "L'ordre doit être un nombre entier");
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}
