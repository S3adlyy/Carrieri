package com.example.guser.controllers.guser;

import com.example.guser.SceneManager;
import entities.guser.User;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.guser.AdminUserService;
import session.SessionContext;
import utils.guser.AlertUtils;

import java.util.List;

public class AdminUsersController {

    @FXML private TextField qField;
    @FXML private ComboBox<String> roleBox;
    @FXML private ComboBox<String> statusBox;

    @FXML private Label feedbackLabel;
    @FXML private Label countBadge;
    @FXML private Label selectedLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Number> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;

    @FXML private ComboBox<String> setRoleBox;

    private final ObservableList<User> data = FXCollections.observableArrayList();
    private final AdminUserService service = new AdminUserService();

    @FXML
    public void initialize() {

        if (!SessionContext.isLoggedIn()
                || SessionContext.getCurrentUser() == null
                || !"ADMIN".equals(SessionContext.getCurrentUser().getRoles())) {
            SceneManager.switchTo("/com/example/guser/guser/login.fxml", "Carrieri • Sign in");
            return;
        }

        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colId.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getId()));
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                safe(c.getValue().getFirstname()) + " " + safe(c.getValue().getLastname())
        ));
        colEmail.setCellValueFactory(c -> new ReadOnlyStringWrapper(safe(c.getValue().getEmail())));
        colRole.setCellValueFactory(c -> new ReadOnlyStringWrapper(safe(c.getValue().getRoles())));
        colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getIsactive() == 1 ? "Active" : "Disabled"
        ));

        usersTable.setItems(data);
        usersTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, o, u) -> {
            selectedLabel.setText(u == null ? "None" : "#" + u.getId() + " • " + safe(u.getEmail()));
        });

        roleBox.getItems().setAll("Any", "Candidate", "Recruiter", "Admin");
        statusBox.getItems().setAll("Any", "Active", "Disabled");
        setRoleBox.getItems().setAll("Candidate", "Recruiter", "Admin");

        roleBox.getSelectionModel().select("Any");
        statusBox.getSelectionModel().select("Any");

        refresh(true);
    }

    @FXML private void onSearch()  { refresh(false); }
    @FXML private void onRefresh() { refresh(true);  }

    @FXML
    private void onClear() {
        qField.clear();
        roleBox.getSelectionModel().select("Any");
        statusBox.getSelectionModel().select("Any");
        setRoleBox.getSelectionModel().clearSelection();
        refresh(false);
    }

    @FXML
    private void onDisable() {
        User u = requireSelection("utilisateur", "désactiver le compte");
        if (u == null) return;

        if (u.getIsactive() == 0) {
            showFeedback("User already disabled.", true);
            AlertUtils.showInfo("Aucune action", "Ce compte est déjà désactivé.");
            return;
        }

        boolean ok = AlertUtils.showConfirmation(
                "Désactiver le compte",
                "Désactiver l'utilisateur : " + safe(u.getEmail()) + " ?",
                "Désactiver",
                "Annuler"
        );
        if (!ok) return;

        try {
            service.setActive(u.getId(), false);
            showFeedback("User disabled.", false);
            AlertUtils.showSuccess("Succès", "Le compte a été désactivé.");
            refresh(false);
        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onEnable() {
        User u = requireSelection("utilisateur", "activer le compte");
        if (u == null) return;

        if (u.getIsactive() == 1) {
            showFeedback("User already active.", true);
            AlertUtils.showInfo("Aucune action", "Ce compte est déjà actif.");
            return;
        }

        boolean ok = AlertUtils.showConfirmation(
                "Activer le compte",
                "Activer l'utilisateur : " + safe(u.getEmail()) + " ?",
                "Activer",
                "Annuler"
        );
        if (!ok) return;

        try {
            service.setActive(u.getId(), true);
            showFeedback("User enabled.", false);
            AlertUtils.showSuccess("Succès", "Le compte a été activé.");
            refresh(false);
        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onApplyRole() {
        User u = requireSelection("utilisateur", "changer le rôle");
        if (u == null) return;

        String role = setRoleBox.getValue();
        if (role == null || role.isBlank()) {
            showFeedback("Pick a role first.", true);
            AlertUtils.showWarning("Rôle manquant", "Veuillez choisir un rôle à appliquer.");
            return;
        }

        String oldRole = safe(u.getRoles());
        if (role.equalsIgnoreCase(oldRole)) {
            showFeedback("User already has this role.", true);
            AlertUtils.showInfo("Aucune action", "L'utilisateur a déjà le rôle : " + role);
            return;
        }

        boolean ok = AlertUtils.showConfirmation(
                "Changer le rôle",
                "Changer le rôle de " + safe(u.getEmail()) + " ?\n\nDe : " + oldRole + "\nVers : " + role,
                "Appliquer",
                "Annuler"
        );
        if (!ok) return;

        try {
            service.setRole(u.getId(), role);
            showFeedback("Role updated.", false);
            AlertUtils.showSuccess("Succès", "Rôle mis à jour vers : " + role);
            refresh(false);
        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onViewProfile() {
        User u = requireSelection("utilisateur", "ouvrir le profil");
        if (u == null) return;

        SessionContext.setProfileTargetUserId(u.getId());
        SessionContext.setAdminViewingProfile(true);

        SceneManager.switchTo("/com/example/guser/guser/profile.fxml", "Carrieri • Profile");
    }

    // ---------------- internals ----------------

    private void refresh(boolean showAlertOnError) {
        try {
            List<User> users = service.search(
                    qField.getText(),
                    roleBox.getValue(),
                    statusBox.getValue()
            );

            data.setAll(users);
            countBadge.setText(String.valueOf(users.size()));
            showFeedback("", false);

        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
            if (showAlertOnError) {
                AlertUtils.showError("Erreur de chargement", e.getMessage());
            }
        }
    }

    private User requireSelection(String elementTypeFr, String actionFr) {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u == null) {
            showFeedback("Select a user first.", true);
            AlertUtils.showNoSelectionWarning(elementTypeFr, actionFr);
            return null;
        }
        return u;
    }

    private void showFeedback(String msg, boolean error) {
        boolean show = msg != null && !msg.isBlank();
        feedbackLabel.setVisible(show);
        feedbackLabel.setManaged(show);
        feedbackLabel.setText(show ? msg : "");

        feedbackLabel.getStyleClass().removeAll("text-danger", "text-success");
        if (show) feedbackLabel.getStyleClass().add(error ? "text-danger" : "text-success");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
