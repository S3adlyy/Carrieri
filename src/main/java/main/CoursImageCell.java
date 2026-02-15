package main;

import entities.Cours;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.CoursService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Optional;

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
                confirmEdit(newImageBytes);
            } catch (Exception ex) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible de lire l'image: " + ex.getMessage());
                cancelEdit();
            }
        } else {
            cancelEdit();
        }
    }

    private void confirmEdit(byte[] newImageBytes) {
        if (confirming) return;
        confirming = true;
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("✏️ Confirmation");
            confirm.setHeaderText("Modifier l'image");
            confirm.setContentText("Voulez-vous remplacer l'image de ce cours ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
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
                } catch (SQLException ex) {
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur base de données: " + ex.getMessage());
                }
            }
            cancelEdit();
        } finally {
            confirming = false;
        }
    }
}

