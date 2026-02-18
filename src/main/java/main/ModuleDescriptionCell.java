package main;

import entities.Module;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import services.ModuleService;
import utils.AlertUtils;

import java.util.Objects;

public class ModuleDescriptionCell extends TableCell<Module, String> {
    private final ModuleService moduleService;
    private TextArea textArea;
    private boolean confirming;

    public ModuleDescriptionCell(ModuleService moduleService) {
        this.moduleService = moduleService;
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
                AlertUtils.showWarning("⚠️ Validation",
                        "La description doit contenir entre 10 et 1000 caractères.\n\n" +
                                "Votre texte actuel: " + newValue.length() + " caractères.");
                cancelEdit();
                return;
            }

            boolean confirmed = AlertUtils.showConfirmation(
                    "✏️ Confirmation",
                    "Êtes-vous sûr de vouloir modifier la description de ce module ?",
                    "Oui, modifier",
                    "Non, annuler"
            );

            if (confirmed) {
                Module module = getTableRow() != null ? getTableRow().getItem() : null;
                if (module == null) {
                    cancelEdit();
                    return;
                }
                module.setDescription(newValue);
                try {
                    moduleService.modifier(module);
                    commitEdit(newValue);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "La description a été modifiée avec succès.");
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