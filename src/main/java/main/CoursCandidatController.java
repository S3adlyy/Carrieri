package main;

import entities.Certification;
import entities.Cours;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.CertificationService;
import services.CoursService;
import services.EmailService;
import services.ProgressionCoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.layout.Priority;
import utils.AlertUtils;
import javafx.stage.DirectoryChooser;
import java.io.File;
import services.TraductionService;
import main.LangueTest;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursCandidatController {
    @FXML private ComboBox<String> comboLangueTest;
    private TraductionService traductionService = new TraductionService();
    private String langueTest = "fr";
    private Map<Integer, Cours> coursTraduits = new HashMap<>();
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
    private int candidatId = 1;

    @FXML
    public void initialize() {
        coursServices = new CoursService();

        comboDomaine.getItems().addAll("Tous", "Développement", "Design", "Data Science", "Marketing");
        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");

        comboDomaine.setValue("Tous");
        comboNiveau.setValue("Tous");

        comboDomaine.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        setupLangueSelector();
        loadCoursFromDatabase();
        chargerStatistiques();

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
    }

    private void loadCoursFromDatabase() {
        try {
            tousLesCours = coursServices.readAll();
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            lblTotalCoursStat.setText(String.valueOf(tousLesCours.size()));
            displayCours(tousLesCours);
        } catch (SQLException e) {
            e.printStackTrace();
            lblTotalCours.setText("0 cours disponibles");
            lblTotalCoursStat.setText("0");
            AlertUtils.showError("❌ Erreur", "Impossible de charger les cours depuis la base de données.");
        }
    }

    private void chargerStatistiques() {
        ProgressionCoursService progressionService = new ProgressionCoursService();
        try {
            int enCours = 0;
            int completes = 0;

            for (Cours c : tousLesCours) {
                double prog = progressionService.getProgressionCours(candidatId, c.getId());
                if (prog >= 99.9) {
                    completes++;
                } else if (prog > 0) {
                    enCours++;
                }
            }

            lblEnCours.setText(String.valueOf(enCours));
            lblCompletes.setText(String.valueOf(completes));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger les statistiques de progression.");
        }
    }

    private void displayCours(List<Cours> list) {
        tileCours.getChildren().clear();
        for (Cours c : list) {
            Cours coursTraduit = traduireCours(c);
            tileCours.getChildren().add(createCoursCard(coursTraduit));
        }
    }

    private VBox createCoursCard(Cours cours) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setSpacing(0);
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setEffect(new DropShadow(10, Color.rgb(94, 84, 142, 0.15)));

        // --- IMAGE DE COUVERTURE ---
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.getStyleClass().add("image-container");

        ImageView imageView;
        if (cours.getImageCouverture() != null && cours.getImageCouverture().length > 0) {
            imageView = new ImageView(new Image(new ByteArrayInputStream(cours.getImageCouverture())));
        } else {
            Label defaultIcon = new Label("📚");
            defaultIcon.getStyleClass().add("default-icon");
            imageContainer.getChildren().add(defaultIcon);
            imageView = new ImageView();
        }

        if (imageView.getImage() != null) {
            imageView.setFitWidth(350);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(350, 160);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            imageContainer.getChildren().add(imageView);
        }

        card.getChildren().add(imageContainer);

        // --- CONTENU DE LA CARTE ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("card-content");

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label lblNiveau = new Label(cours.getNiveau());
        lblNiveau.getStyleClass().addAll("level-badge", getLevelStyle(cours.getNiveau()));

        Label lblCategorie = new Label(cours.getCompetences_visees() != null ?
                cours.getCompetences_visees().split(",")[0] : "Général");
        lblCategorie.getStyleClass().add("category-badge");

        badges.getChildren().addAll(lblNiveau, lblCategorie);

        Label lblTitre = new Label(cours.getTitre());
        lblTitre.getStyleClass().add("course-title-label");
        lblTitre.setWrapText(true);

        Label lblDesc = new Label(cours.getDescription());
        lblDesc.getStyleClass().add("course-description-label");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(60);

        HBox metaBox = new HBox(20);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label clockIcon = new Label("⏱");
        clockIcon.setStyle("-fx-font-size: 14px;");
        Label lblDuration = new Label(cours.getDuree() + " heures");
        lblDuration.getStyleClass().add("duration-label");
        durationBox.getChildren().addAll(clockIcon, lblDuration);

        metaBox.getChildren().add(durationBox);

        if (cours.isEst_obligatoire()) {
            HBox obligBox = new HBox(5);
            obligBox.setAlignment(Pos.CENTER_LEFT);
            Label obligIcon = new Label("📌");
            obligIcon.setStyle("-fx-font-size: 14px;");
            Label lblOblig = new Label("Obligatoire");
            lblOblig.getStyleClass().add("obligatoire-label");
            obligBox.getChildren().addAll(obligIcon, lblOblig);
            metaBox.getChildren().add(obligBox);
        }

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnCommencer = new Button("Commencer");
        btnCommencer.getStyleClass().add("btn-commencer");

        btnCommencer.setOnAction(e -> ouvrirCours(cours));
        btnCommencer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCommencer, Priority.ALWAYS);

        Button btnPDF = new Button("PDF");
        btnPDF.getStyleClass().add("btn-pdf");
        btnPDF.setOnAction(event -> genererCertifTest(cours));

        buttonBox.getChildren().addAll(btnCommencer, btnPDF);

        content.getChildren().addAll(badges, lblTitre, lblDesc, metaBox, buttonBox);
        card.getChildren().add(content);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(94,84,142,0.3), 20, 0, 0, 5);");
            card.setTranslateY(-3);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(94,84,142,0.15), 10, 0, 0, 2);");
            card.setTranslateY(0);
        });

        return card;
    }

    private String getLevelStyle(String niveau) {
        if (niveau == null) return "level-intermediaire";
        switch (niveau.toLowerCase()) {
            case "débutant": return "level-debutant";
            case "intermédiaire": return "level-intermediaire";
            case "avancé": return "level-avance";
            case "expert": return "level-expert";
            case "master": return "level-master";
            default: return "level-intermediaire";
        }
    }

    private void ouvrirCours(Cours cours) {
        CandidatShellController.getInstance().openCours(cours);
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
                .collect(Collectors.toList());

        displayCours(filtered);
        lblTotalCours.setText(filtered.size() + " cours disponibles");
    }

    // ============================================
    // GÉNÉRER CERTIFICAT - MODIFIÉ
    // ============================================
    private void genererCertifTest(Cours cours) {
        if (cours == null) {
            AlertUtils.showWarning("⚠️ Attention", "Cours invalide !");
            return;
        }

        ProgressionCoursService progressionService = new ProgressionCoursService();

        try {
            double progression = progressionService.getProgressionCours(candidatId, cours.getId());
            boolean estComplete = progression >= 99.9;

            if (!estComplete) {
                AlertUtils.showWarning("⚠️ Cours non terminé",
                        "Progression: " + String.format("%.0f%%", progression));
                return;
            }

            CertificationService certifService = new CertificationService();
            Certification existing = certifService.readByCoursAndCandidat(cours.getId(), candidatId);

            if (existing != null) {
                String date = existing.getDateObtention()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));

                AlertUtils.showInfo("📄 Certificat déjà généré",
                        "Vous avez déjà un certificat pour ce cours (généré le " + date + ").");
                return;
            }

            boolean confirmed = AlertUtils.showConfirmation(
                    "🎓 Génération du certificat",
                    "Félicitations ! Vous avez complété le cours \"" + cours.getTitre() + "\" à 100%.\n\n" +
                            "Voulez-vous générer votre certificat ?",
                    "Oui",
                    "Non"
            );

            if (confirmed) {
                String nomCandidat = "Bilal Eter";
                CertificationService pdfService = new CertificationService();

                try {
                    // 1. Créer l'entrée en base
                    Certification certif = new Certification(
                            candidatId,
                            cours.getId(),
                            java.time.LocalDateTime.now()
                    );
                    int certificatId = pdfService.ajouterEtRetournerId(certif);

                    if (certificatId > 0) {
                        // 2. Générer le PDF professionnel
                        byte[] pdfBytes = genererPDFProfessionnel(nomCandidat, cours.getTitre(), certificatId);

                        // 3. Stocker en base
                        String sql = "UPDATE certification SET fichier_pdf = ? WHERE id = ?";
                        try (java.sql.PreparedStatement ps = utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
                            ps.setBytes(1, pdfBytes);
                            ps.setInt(2, certificatId);
                            ps.executeUpdate();
                            System.out.println("✅ PDF stocké en base pour le certificat ID: " + certificatId);
                        }

                        // 4. Email
                        envoyerEmailNotification(nomCandidat, cours.getTitre(), "Certificat stocké en base");

                        AlertUtils.showSuccessWithInstructions(
                                "🎉 FÉLICITATIONS !",
                                "Votre certificat professionnel a été généré avec succès.",
                                "📧 Un email de confirmation a été envoyé.\n\n" +
                                        "Vous pourrez télécharger votre certificat à tout moment depuis la page 'Mes certificats'.\n\n" +
                                        "Continuez sur votre lancée ! 🚀"
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    AlertUtils.showError("❌ Erreur", "Erreur :\n" + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", e.getMessage());
        }
    }
    /**
     * Génère un certificat professionnel sur une seule page
     */
    /**
     * Génère un certificat professionnel sur une seule page avec le logo Carrieri
     */
    /**
     * Génère un certificat professionnel sur une seule page avec le logo Carrieri (image)
     */
    private byte[] genererPDFProfessionnel(String nomCandidat, String titreCours, int certificatId) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);

        // Format A4 portrait (une seule page)
        pdf.setDefaultPageSize(com.itextpdf.kernel.geom.PageSize.A4);

        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
        document.setMargins(40, 40, 40, 40);

        // Couleurs de Carrieri
        com.itextpdf.kernel.colors.Color violetFonce = new com.itextpdf.kernel.colors.DeviceRgb(35, 25, 66);  // #231942
        com.itextpdf.kernel.colors.Color violet = new com.itextpdf.kernel.colors.DeviceRgb(94, 84, 142);      // #5E548E
        com.itextpdf.kernel.colors.Color violetClair = new com.itextpdf.kernel.colors.DeviceRgb(159, 134, 192); // #9F86C0
        com.itextpdf.kernel.colors.Color rose = new com.itextpdf.kernel.colors.DeviceRgb(224, 177, 203);      // #E0B1CB

        // --- BORDURE DÉCORATIVE EN CADRE ---
        float[] borderWidths = {1};
        com.itextpdf.layout.element.Table borderTable = new com.itextpdf.layout.element.Table(borderWidths);
        borderTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        com.itextpdf.layout.element.Cell borderCell = new com.itextpdf.layout.element.Cell();
        borderCell.setBorder(new com.itextpdf.layout.borders.SolidBorder(violet, 2));
        borderCell.setPadding(20);

        // --- LOGO IMAGE (DEPUIS LES RESSOURCES) ---
        try {
            // Charger l'image depuis les ressources
            java.io.InputStream imageStream = getClass().getResourceAsStream("/images/logo.png");
            if (imageStream != null) {
                byte[] imageBytes = imageStream.readAllBytes();
                com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(imageBytes);
                com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(imageData);

                // Redimensionner le logo (hauteur 60, largeur automatique)
                logo.setHeight(60);
                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                logo.setMarginTop(10);
                logo.setMarginBottom(5);

                borderCell.add(logo);
            } else {
                // Fallback: texte si l'image n'est pas trouvée
                com.itextpdf.layout.element.Paragraph fallbackLogo = new com.itextpdf.layout.element.Paragraph("Carrieri")
                        .setFontSize(48)
                        .setFontColor(violetFonce)
                        .setBold()
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setMarginTop(10)
                        .setMarginBottom(5);
                borderCell.add(fallbackLogo);
                System.out.println("⚠️ Logo non trouvé, utilisation du texte par défaut");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback en cas d'erreur
            com.itextpdf.layout.element.Paragraph fallbackLogo = new com.itextpdf.layout.element.Paragraph("Carrieri")
                    .setFontSize(48)
                    .setFontColor(violetFonce)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setMarginBottom(5);
            borderCell.add(fallbackLogo);
        }

        // --- TITRE PRINCIPAL ---
        com.itextpdf.layout.element.Paragraph titreCertif = new com.itextpdf.layout.element.Paragraph("CERTIFICAT DE RÉUSSITE")
                .setFontSize(28)
                .setFontColor(violet)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(20);
        borderCell.add(titreCertif);

        // --- LIGNE DÉCORATIVE ---
        com.itextpdf.layout.element.LineSeparator ligne = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(2f));
        ligne.setWidth(150);
        ligne.setMarginBottom(25);
        ligne.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        borderCell.add(ligne);

        // --- TEXTE "DÉCERNÉ À" ---
        com.itextpdf.layout.element.Paragraph decerne = new com.itextpdf.layout.element.Paragraph("Ce certificat est décerné à")
                .setFontSize(14)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(decerne);

        // --- NOM DU CANDIDAT (ENCADRÉ) ---
        com.itextpdf.layout.element.Paragraph nom = new com.itextpdf.layout.element.Paragraph(nomCandidat)
                .setFontSize(30)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(250, 247, 252)) // Fond très léger
                .setPadding(12)
                .setMarginBottom(15);
        borderCell.add(nom);

        // --- TEXTE "POUR AVOIR COMPLÉTÉ" ---
        com.itextpdf.layout.element.Paragraph pour = new com.itextpdf.layout.element.Paragraph("pour avoir complété avec succès le cours")
                .setFontSize(14)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(pour);

        // --- TITRE DU COURS (STYLISÉ) ---
        com.itextpdf.layout.element.Paragraph coursTitre = new com.itextpdf.layout.element.Paragraph(titreCours)
                .setFontSize(22)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(25);
        borderCell.add(coursTitre);

        // --- MENTION ---
        com.itextpdf.layout.element.Paragraph mention = new com.itextpdf.layout.element.Paragraph("avec la mention ⭐ FÉLICITATIONS ⭐")
                .setFontSize(14)
                .setFontColor(rose)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(30);
        borderCell.add(mention);

        // --- INFORMATIONS (DATE ET NUMÉRO) ---
        float[] columnWidths = {1, 1};
        com.itextpdf.layout.element.Table infoTable = new com.itextpdf.layout.element.Table(columnWidths);
        infoTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(80));
        infoTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        infoTable.setMarginBottom(25);

        // Date d'obtention
        com.itextpdf.layout.element.Cell dateCell = new com.itextpdf.layout.element.Cell();
        String dateFormatee = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        dateCell.add(new com.itextpdf.layout.element.Paragraph("Date").setFontColor(violet).setFontSize(12).setBold());
        dateCell.add(new com.itextpdf.layout.element.Paragraph(dateFormatee).setFontColor(violetFonce).setFontSize(14));
        dateCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        dateCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        dateCell.setPadding(5);

        // Numéro de certificat
        com.itextpdf.layout.element.Cell numeroCell = new com.itextpdf.layout.element.Cell();
        numeroCell.add(new com.itextpdf.layout.element.Paragraph("N° certificat").setFontColor(violet).setFontSize(12).setBold());
        numeroCell.add(new com.itextpdf.layout.element.Paragraph("CERT-" + String.format("%04d", certificatId)).setFontColor(violetFonce).setFontSize(14));
        numeroCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        numeroCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        numeroCell.setPadding(5);

        infoTable.addCell(dateCell);
        infoTable.addCell(numeroCell);

        borderCell.add(infoTable);

        // --- LIGNE DE SÉPARATION ---
        com.itextpdf.layout.element.LineSeparator ligneFine = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f));
        ligneFine.setWidth(400);
        ligneFine.setMarginBottom(20);
        ligneFine.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        borderCell.add(ligneFine);

        // --- SIGNATURE UNIQUE (BILAL EL ETER) ---
        com.itextpdf.layout.element.Paragraph signature = new com.itextpdf.layout.element.Paragraph("Bilal El Eter")
                .setFontSize(18)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(signature);

        com.itextpdf.layout.element.Paragraph signatureTitre = new com.itextpdf.layout.element.Paragraph("Formateur & Directeur Pédagogique")
                .setFontSize(12)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(signatureTitre);

        // --- PIED DE PAGE ---
        com.itextpdf.layout.element.Paragraph footer = new com.itextpdf.layout.element.Paragraph("Carrieri · www.carrieri.com")
                .setFontSize(10)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginTop(10);
        borderCell.add(footer);

        borderTable.addCell(borderCell);
        document.add(borderTable);
        document.close();

        return baos.toByteArray();
    }
    /**
     * Génère un PDF de certificat professionnel avec le logo Carrieri
     */

    // ============================================
// ENVOI EMAIL DE NOTIFICATION
// ============================================
    private void envoyerEmailNotification(String nomCandidat, String titreCours, String cheminCertificat) {
        try {
            String emailDestinataire = "bilaleter05@gmail.com"; // Votre email

            System.out.println("📧 Tentative d'envoi d'email depuis le catalogue...");

            EmailService emailService = new EmailService();

            boolean envoye = emailService.envoyerNotificationCompletionCours(
                    emailDestinataire,
                    nomCandidat,
                    titreCours,
                    "file:///" + cheminCertificat.replace("\\", "/")
            );

            if (envoye) {
                System.out.println("✅ Email envoyé avec succès depuis le catalogue");
            } else {
                System.err.println("❌ Échec envoi email depuis le catalogue");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void rechercherCours() {
        filtrerCours();
    }

    private void setupLangueSelector() {
        // Initialiser le ComboBox
        comboLangueTest.setItems(FXCollections.observableArrayList(
                "🇫🇷 Français (fr)",
                "🇬🇧 English (en)",
                "🇪🇸 Español (es)",
                "🇩🇪 Deutsch (de)",
                "🇮🇹 Italiano (it)",
                "🇵🇹 Português (pt)",
                "🇳🇱 Nederlands (nl)",
                "🇷🇺 Русский (ru)",
                "🇨🇳 中文 (zh-Hans)",
                "🇯🇵 日本語 (ja)",
                "🇰🇷 한국어 (ko)",
                "🇸🇦 العربية (ar)",
                "🇮🇳 हिन्दी (hi)"
        ));

        // Récupérer la langue depuis LangueTest
        langueTest = LangueTest.getInstance().getLangue();

        // Sélectionner la bonne valeur
        for (String item : comboLangueTest.getItems()) {
            if (item.contains("(" + langueTest + ")")) {
                comboLangueTest.setValue(item);
                break;
            }
        }

        comboLangueTest.setOnAction(e -> changerLangue());
    }
    @FXML
    private void changerLangue() {
        String selection = comboLangueTest.getValue();
        if (selection != null && selection.contains("(")) {
            langueTest = selection.substring(
                    selection.indexOf("(") + 1,
                    selection.indexOf(")")
            );

            LangueTest.getInstance().setLangue(langueTest);
            rechargerCoursAvecTraduction();

            AlertUtils.showInfo("🌐 Langue changée",
                    "Les cours sont maintenant en " + selection.split(" ")[0]);
        }
    }
    private void rechargerCoursAvecTraduction() {
        try {
            tousLesCours = coursServices.readAll();
            coursTraduits.clear();
            displayCours(tousLesCours);
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            System.out.println(traductionService.getStatistiques());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private Cours traduireCours(Cours cours) {
        if (coursTraduits.containsKey(cours.getId())) {
            return coursTraduits.get(cours.getId());
        }

        Cours coursTraduit = new Cours(
                traductionService.traduire(cours.getTitre(), langueTest),
                traductionService.traduire(cours.getDescription(), langueTest),
                cours.getDuree(),
                cours.getNiveau(),
                traductionService.traduire(cours.getCompetences_visees(), langueTest),
                cours.isEst_obligatoire(),
                cours.getCreatedBy(),
                cours.getImageCouverture()
        );
        coursTraduit.setId(cours.getId());

        coursTraduits.put(cours.getId(), coursTraduit);
        return coursTraduit;
    }
}