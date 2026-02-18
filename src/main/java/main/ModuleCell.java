package main;

import entities.Module;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import main.MainShellController;
import utils.AlertUtils;
import services.ModuleService;

public class ModuleCell {  // ✅ Plus d'import SQLException

    private static final ModuleService moduleService = new ModuleService();

    // ============================================
    // CELLULE POUR LE TITRE
    // ============================================
    public static TableCell<Module, String> titleCell() {
        return new TableCell<Module, String>() {
            private TextField textField;

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

            @Override
            public void startEdit() {
                super.startEdit();
                textField = new TextField(getItem());
                textField.setOnAction(e -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) commitEdit(textField.getText());
                });
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() < 3 || newValue.length() > 200) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "Le titre doit contenir entre 3 et 200 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Module module = getTableView().getItems().get(getIndex());
                String oldValue = module.getTitre();

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification du titre",
                        "De: \"" + oldValue + "\"\nVers: \"" + newValue + "\"",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    module.setTitre(newValue);
                    try {
                        moduleService.modifier(module);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LA DESCRIPTION
    // ============================================
    public static TableCell<Module, String> descriptionCell() {
        return new TableCell<Module, String>() {
            private TextArea textArea;

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
                        setText(item != null ? item.substring(0, Math.min(30, item.length())) + "..." : "");
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textArea = new TextArea(getItem());
                textArea.setWrapText(true);
                textArea.setPrefRowCount(4);
                textArea.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) commitEdit(textArea.getText());
                });
                setText(null);
                setGraphic(textArea);
                textArea.requestFocus();
            }

            @Override
            public void commitEdit(String newValue) {
                if (newValue.length() < 10 || newValue.length() > 1000) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "La description doit contenir entre 10 et 1000 caractères.\n" +
                                    "Valeur saisie: " + newValue.length() + " caractères.");
                    cancelEdit();
                    return;
                }

                Module module = getTableView().getItems().get(getIndex());

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification de la description",
                        "Êtes-vous sûr de vouloir modifier la description ?",
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    module.setDescription(newValue);
                    try {
                        moduleService.modifier(module);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR L'ORDRE
    // ============================================
    public static TableCell<Module, Integer> ordreCell(ObservableList<Module> moduleList) {
        return new TableCell<Module, Integer>() {
            private TextField textField;

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

            @Override
            public void startEdit() {
                super.startEdit();
                textField = new TextField(getItem().toString());
                textField.textProperty().addListener((obs, old, newVal) -> {
                    if (!newVal.matches("\\d*")) {
                        textField.setText(newVal.replaceAll("[^\\d]", ""));
                    }
                });
                textField.setOnAction(e -> {
                    try {
                        commitEdit(Integer.parseInt(textField.getText()));
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarning("⚠️ Validation", "Veuillez entrer un nombre valide.");
                    }
                });
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void commitEdit(Integer newValue) {
                if (newValue <= 0 || newValue > 100) {
                    AlertUtils.showWarning("⚠️ Validation",
                            "L'ordre doit être compris entre 1 et 100.\n" +
                                    "Valeur saisie: " + newValue);
                    cancelEdit();
                    return;
                }

                Module module = getTableView().getItems().get(getIndex());
                Integer oldValue = module.getOrdre();

                boolean ordreExiste = moduleList.stream()
                        .anyMatch(m -> m.getOrdre() == newValue && m.getId() != module.getId());

                if (ordreExiste) {
                    AlertUtils.showWarning("⚠️ Ordre déjà utilisé",
                            "Un autre module a déjà l'ordre " + newValue + ".\n" +
                                    "Veuillez choisir un autre ordre.");
                    cancelEdit();
                    return;
                }

                boolean confirmed = AlertUtils.showConfirmation(
                        "✏️ Modification de l'ordre",
                        "De: " + oldValue + "\nVers: " + newValue,
                        "Oui, modifier",
                        "Non, annuler"
                );

                if (confirmed) {
                    module.setOrdre(newValue);
                    try {
                        moduleService.modifier(module);
                        super.commitEdit(newValue);
                        getTableView().refresh();
                    } catch (Exception e) {  // ✅ Changé de SQLException à Exception
                        AlertUtils.showError("❌ Erreur", "Erreur lors de la modification:\n" + e.getMessage());
                    }
                } else {
                    cancelEdit();
                }
            }
        };
    }

    // ============================================
    // CELLULE POUR LES ACTIONS
    // ============================================
    public static TableCell<Module, Void> actionsCell() {
        return new TableCell<Module, Void>() {
            private final Button btnLecons = new Button("📖");
            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(5, btnLecons, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);

                btnLecons.setStyle("-fx-background-color: #9F86C0; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                btnLecons.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    MainShellController.getInstance().setCurrentModule(module.getId(), module.getTitre());
                    MainShellController.getInstance().showLeconsViewWithModule(module.getId(), module.getTitre());
                });

                btnDelete.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    // La logique de suppression sera dans le contrôleur
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        };
    }
}