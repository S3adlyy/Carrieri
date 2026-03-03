package com.example.guser.controllers.getude;

import entities.getude.Module;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import services.getude.ModuleService;
import utils.getude.AlertUtils;

public class ModuleCell {  // ✅ Plus d'import SQLException

    private static final ModuleService moduleService = new ModuleService();

    // ============================================
    // CELLULE TITRE
    // ============================================
    public static TableCell<Module, String> getTitleCell() {
        return new ModuleTitleCellImpl();
    }

    private static class ModuleTitleCellImpl extends TableCell<Module, String> {
        private TextField textField;
        private boolean confirming;

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

                if (newValue.equals(oldValue)) {
                    cancelEdit();
                    return;
                }

                if (newValue.length() < 3 || newValue.length() > 200) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le titre du module doit contenir entre 3 et 200 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
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
                        AlertUtils.showSuccess("✅ Succès", "Titre du module modifié avec succès.");
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE DESCRIPTION
    // ============================================
    public static TableCell<Module, String> getDescriptionCell() {
        return new ModuleDescriptionCellImpl();
    }

    private static class ModuleDescriptionCellImpl extends TableCell<Module, String> {
        private TextArea textArea;
        private boolean confirming;

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

                if (newValue.equals(oldValue)) {
                    cancelEdit();
                    return;
                }

                if (newValue.length() < 10 || newValue.length() > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La description du module doit contenir entre 10 et 1000 caractères.\n\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "Êtes-vous sûr de vouloir modifier la description du module ?",
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
                        AlertUtils.showSuccess("✅ Succès", "Description du module modifiée avec succès.");
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }

    // ============================================
    // CELLULE ORDRE
    // ============================================
    public static TableCell<Module, Integer> getOrdreCell(ObservableList<Module> moduleList) {
        return new ModuleOrdreCellImpl(moduleList);
    }

    private static class ModuleOrdreCellImpl extends TableCell<Module, Integer> {
        private final ObservableList<Module> moduleList;
        private TextField textField;
        private boolean confirming;

        public ModuleOrdreCellImpl(ObservableList<Module> moduleList) {
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
            textField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    textField.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    try {
                        confirmEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarning("⚠️ Validation", "Veuillez entrer un nombre valide.");
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    try {
                        confirmEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        cancelEdit();
                    }
                }
            });
        }

        private void confirmEdit(int newValue) {
            if (confirming) return;
            confirming = true;
            try {
                Integer oldValue = getItem();

                if (newValue == oldValue) {
                    cancelEdit();
                    return;
                }

                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "L'ordre doit être compris entre 1 et 100.\n\n" +
                                    "Valeur saisie: " + newValue);
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
                    AlertUtils.showWarning("⚠️ Ordre déjà utilisé",
                            "Un autre module a déjà l'ordre " + newValue + ".\n\n" +
                                    "Veuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Confirmation",
                        "De: " + oldValue + "\nVers: " + newValue,
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    module.setOrdre(newValue);
                    try {
                        moduleService.modifier(module);
                        commitEdit(newValue);
                        getTableView().refresh();
                        AlertUtils.showSuccess("✅ Succès", "Ordre du module modifié avec succès.");
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + e.getMessage());
                    }
                }
                cancelEdit();
            } finally {
                confirming = false;
            }
        }
    }
}