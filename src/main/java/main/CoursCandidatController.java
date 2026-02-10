package main;

import entities.Certification;
import entities.Cours;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.CertificationPDFService;
import services.CertificationService;
import services.CoursService;
import services.ProgressionCoursService;
import services.IProgressionCoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.shape.Rectangle;
import java.io.ByteArrayInputStream;




import java.sql.SQLException;
import java.util.List;

public class CoursCandidatController {

    @FXML private Label lblTotalCours;
    @FXML private Label lblTotalCoursStat;
    @FXML private Label lblEnCours;
    @FXML private Label lblCompletes;
    @FXML private TextField txtRecherche;
    @FXML private TilePane tileCours;
    @FXML private ComboBox<String> comboDomaine;
    @FXML private ComboBox<String> comboNiveau;

    private CoursService coursServices;
    private List<Cours> tousLesCours;


    @FXML
    public void initialize() {
        coursServices = new CoursService();

        // Initialiser les ComboBox
        comboDomaine.getItems().addAll("Tous", "Développement Web", "Design", "Data Science", "Marketing");
        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");

        comboDomaine.setValue("Tous");
        comboNiveau.setValue("Tous");

        // Écouteurs pour les filtres
        comboDomaine.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());

        loadCoursFromDatabase();

        // Recherche dynamique
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
    }

    private void loadCoursFromDatabase() {
        try {
            tousLesCours = coursServices.read();
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            lblTotalCoursStat.setText(String.valueOf(tousLesCours.size()));
            displayCours(tousLesCours);
        } catch (SQLException e) {
            e.printStackTrace();
            lblTotalCours.setText("0 cours disponibles");
            lblTotalCoursStat.setText("0");
        }
    }

    private void displayCours(List<Cours> list) {
        tileCours.getChildren().clear();
        for (Cours c : list) {
            tileCours.getChildren().add(createCoursCard(c));
        }
    }

    private VBox createCoursCard(Cours cours) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setSpacing(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(340);
        card.setMaxWidth(340);

        // --- IMAGE DE COUVERTURE ---
        ImageView imageView;
        if (cours.getImageCouverture() != null && cours.getImageCouverture().length > 0) {
            imageView = new ImageView(new Image(new ByteArrayInputStream(cours.getImageCouverture())));
        } else {
            imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
        }
        imageView.setFitWidth(340); // largeur = largeur du card
        imageView.setFitHeight(160); // hauteur max souhaitée
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        // coins arrondis
        Rectangle clip = new Rectangle(340, 160);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        card.getChildren().add(imageView);



        // --- HEADER ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label lblNiveau = new Label(cours.getNiveau());
        lblNiveau.getStyleClass().add("level-badge");

        Label lblDomaine = new Label(cours.getCompetences_visees() != null ?
                cours.getCompetences_visees().split(",")[0] : "Général");
        lblDomaine.getStyleClass().add("course-domain");
        HBox.setMargin(lblDomaine, new Insets(0, 0, 0, 10));

        header.getChildren().addAll(lblNiveau, lblDomaine);

        // --- TITRE ET DESCRIPTION ---
        Label lblTitre = new Label(cours.getTitre());
        lblTitre.getStyleClass().add("course-title");
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(300);

        Label lblDesc = new Label(cours.getDescription());
        lblDesc.getStyleClass().add("course-description");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(300);
        lblDesc.setMaxHeight(60);

        // --- MÉTADONNÉES ---
        HBox metaBox = new HBox(15);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Label lblDuration = new Label("⏱️ " + cours.getDuree() + "h");
        lblDuration.getStyleClass().add("course-meta");
        metaBox.getChildren().add(lblDuration);

        if (cours.isEst_obligatoire()) {
            Label lblOblig = new Label("📌 Obligatoire");
            lblOblig.getStyleClass().add("course-obligatoire");
            metaBox.getChildren().add(lblOblig);
        }

        // --- BOUTONS ---
        Button btn = new Button("Commencer le cours");
        btn.setOnAction(e -> ouvrirCours(cours));
        btn.getStyleClass().add("course-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);

        Button btnPDF = new Button("Générer PDF");
        btnPDF.getStyleClass().add("course-button-pdf");
        btnPDF.setMaxWidth(Double.MAX_VALUE);
        btnPDF.setPrefHeight(35);
        btnPDF.setOnAction(event -> genererCertifTest(cours));

        // --- ASSEMBLER LES NŒUDS ---
        card.getChildren().addAll(header, lblTitre, lblDesc, metaBox, btn, btnPDF);

        return card;
    }

    private void ouvrirCours(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CoursPlayer.fxml"));
            Parent root = loader.load();

            CoursPlayerController controller = loader.getController();
            controller.setCours(cours);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(cours.getTitre());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void filtrerCours() {
        String recherche = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";
        String domaine = comboDomaine.getValue();
        String niveau = comboNiveau.getValue();

        List<Cours> filtered = tousLesCours.stream()
                .filter(c -> {
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getTitre() != null && c.getTitre().toLowerCase().contains(recherche)) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));
                    boolean matchesDomaine = "Tous".equals(domaine) ||
                            (c.getCompetences_visees() != null &&
                                    c.getCompetences_visees().toLowerCase().contains(domaine.toLowerCase()));
                    boolean matchesNiveau = "Tous".equals(niveau) ||
                            (c.getNiveau() != null &&
                                    c.getNiveau().toLowerCase().contains(niveau.toLowerCase()));

                    return matchesSearch && matchesDomaine && matchesNiveau;
                })
                .distinct() // évite doublons au cas où
                .toList();

        displayCours(filtered);
        lblTotalCours.setText(filtered.size() + " cours disponibles");
    }


    private void genererCertifTest(Cours cours) {

        if (cours == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Cours invalide !");
            return;
        }

        int candidatId = 1; // utilisateur fixe (pas de session)

        IProgressionCoursService progressionService = new ProgressionCoursService();

        try {

            // Vérifier si le cours est complété à 100%
            boolean estComplete = progressionService.estCoursComplete(candidatId, cours.getId());

            if (!estComplete) {
                showAlert(Alert.AlertType.ERROR,
                        "Cours non terminé",
                        "Vous devez compléter ce cours à 100% pour générer le certificat.");
                return;
            }

            // Si complet → générer certificat
            String candidatName = "Bilal Eter";

            String cheminFichier =
                    "C:/Users/MSI/Desktop/certif/certif_" + cours.getTitre() + ".pdf";

            CertificationPDFService pdfService = new CertificationPDFService();
            pdfService.genererCertification(candidatName, cours.getTitre(), cheminFichier);

            showAlert(Alert.AlertType.INFORMATION,
                    "✅ Succès",
                    "Certificat généré : " + cheminFichier);

            // Enregistrer en base
            CertificationService certService = new CertificationService();
            Certification certif = new Certification(
                    candidatId,
                    cours.getId(),
                    java.time.LocalDateTime.now()
            );
            certService.ajouter(certif);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la génération du certificat");
        }
    }




    @FXML
    private void rechercherCours() {
        filtrerCours();
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);

        // Style personnalisé selon le type
        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            // Si le CSS n'existe pas, on continue sans
        }

        alert.showAndWait();
    }
}
