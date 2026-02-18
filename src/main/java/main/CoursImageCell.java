package main;

import entities.Cours;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.CoursService;
import utils.AlertUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;

public class CoursImageCell extends TableCell<Cours, byte[]> {
    private final CoursService coursService;
    private final ImageView imageView = new ImageView();
    private boolean confirming;

    public CoursImageCell(CoursService coursService) {
        this.coursService = coursService;
        imageView.setFitWidth(80);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
        super.startEdit();
        chooseImage();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), false);
    }

    @Override
    protected void updateItem(byte[] imageBytes, boolean empty) {
        super.updateItem(imageBytes, empty);
        if (empty || imageBytes == null) {
            setGraphic(null);
        } else {
            imageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
            setGraphic(imageView);
        }
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une nouvelle image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(getTableView().getScene().getWindow());

        if (file != null) {
            try {
                byte[] newImageBytes = Files.readAllBytes(file.toPath());
                confirmEdit(newImageBytes, file.getName());
            } catch (Exception ex) {
                AlertUtils.showError("❌ Erreur", "Impossible de lire l'image:\n\n" + ex.getMessage());
                cancelEdit();
            }
        } else {
            cancelEdit();
        }
    }

    private void confirmEdit(byte[] newImageBytes, String fileName) {
        if (confirming) return;
        confirming = true;
        try {
            boolean confirmed = AlertUtils.showConfirmation(
                    "✏️ Confirmation",
                    "Voulez-vous remplacer l'image de ce cours ?\n\n" +
                            "Nouvelle image: \"" + fileName + "\"\n" +
                            "Taille: " + formatTaille(newImageBytes.length),
                    "Oui, remplacer",
                    "Non, annuler"
            );

            if (confirmed) {
                Cours cours = getTableRow() != null ? getTableRow().getItem() : null;
                if (cours == null) {
                    cancelEdit();
                    return;
                }
                cours.setImageCouverture(newImageBytes);
                try {
                    coursService.update(cours);
                    commitEdit(newImageBytes);
                    getTableView().refresh();
                    AlertUtils.showSuccess("✅ Succès", "L'image a été modifiée avec succès.");
                } catch (SQLException ex) {
                    AlertUtils.showError("❌ Erreur", "Erreur base de données:\n\n" + ex.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }

    private String formatTaille(long taille) {
        if (taille < 1024) return taille + " B";
        if (taille < 1024 * 1024) return (taille / 1024) + " KB";
        return String.format("%.1f MB", taille / (1024.0 * 1024.0));
    }
}