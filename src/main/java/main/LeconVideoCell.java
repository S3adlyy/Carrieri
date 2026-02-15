package main;

import entities.Lecon;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import services.LeconService;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Optional;

public class LeconVideoCell extends TableCell<Lecon, byte[]> {
    private final LeconService leconService;
    private boolean confirming;

    public LeconVideoCell(LeconService leconService) {
        this.leconService = leconService;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        chooseVideo();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), false);
    }

    @Override
    protected void updateItem(byte[] videoBytes, boolean empty) {
        super.updateItem(videoBytes, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (videoBytes != null && videoBytes.length > 0) {
                setText("🎥 " + formatTaille(videoBytes.length));
            } else {
                setText("❌");
            }
            setGraphic(null);
        }
    }

    private String formatTaille(long taille) {
        if (taille < 1024) {
            return taille + " B";
        } else if (taille < 1024 * 1024) {
            return (taille / 1024) + " KB";
        } else if (taille < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", taille / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", taille / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void chooseVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une nouvelle vidéo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos MP4", "*.mp4"),
                new FileChooser.ExtensionFilter("Tous les formats", "*.mp4", "*.avi", "*.mov", "*.mkv")
        );
        File file = fileChooser.showOpenDialog(getTableView().getScene().getWindow());

        if (file != null) {
            try {
                byte[] newVideoBytes = Files.readAllBytes(file.toPath());

                // Vérifier la taille
                if (newVideoBytes.length > 64 * 1024 * 1024) { // 64 MB max
                    AlertUtils.showAlert(Alert.AlertType.WARNING, "⚠️ Attention",
                        "La vidéo ne peut pas dépasser 64 MB (limite MySQL).\nTaille: " + formatTaille(newVideoBytes.length));
                    cancelEdit();
                    return;
                }

                confirmEdit(newVideoBytes, file.getName());
            } catch (Exception ex) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible de lire la vidéo: " + ex.getMessage());
                cancelEdit();
            }
        } else {
            cancelEdit();
        }
    }

    private void confirmEdit(byte[] newVideoBytes, String fileName) {
        if (confirming) return;
        confirming = true;
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier la vidéo");
            confirm.setContentText("Voulez-vous remplacer la vidéo de cette leçon ?\n\nNouvelle vidéo: " + fileName + " (" + formatTaille(newVideoBytes.length) + ")");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                if (lecon == null) {
                    cancelEdit();
                    return;
                }
                lecon.setVideo(newVideoBytes);
                leconService.modifier(lecon);
                commitEdit(newVideoBytes);
                getTableView().refresh();
                AlertUtils.showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Vidéo modifiée avec succès !");
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}


